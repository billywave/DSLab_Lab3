package security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Arrays;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

/**
 *
 * @author Bernhard
 */
public class SecureChannel implements Channel {
	private Logger logger = Logger.getLogger(this.getClass());
	
	private Channel channel;
	private RSAChannel rsaChannel;
	private AESChannel aesChannel;
	private final Socket socket;
	
	private String localChallenge;
	private String remoteChallenge;
	
	private String loginMessage;
	private int waitingForMessage;
	
	private final String systemName = "system";
	private String loginName;
	private boolean authorized;
	private boolean pendingList;

	public SecureChannel(Socket socket) {
		this.socket = socket;
		reset();
		
	}
	
	/**
	 * Sets the channel back to handle logins
	 */
	private void reset() {
		authorized = false;
		pendingList = false;
		loginMessage = null;
		waitingForMessage = 1;
		loginName = null;
		
		Base64Channel base64Channel = new Base64Channel(new TCPChannel(socket));
		this.rsaChannel = new RSAChannel(base64Channel);
		this.aesChannel = new AESChannel(base64Channel);
		this.channel = rsaChannel;
		
		// generates a 32 byte secure random number
		SecureRandom secureRandom = new SecureRandom();
		final byte[] number = new byte[32];
		secureRandom.nextBytes(number);
		localChallenge = new String(Base64.encode(number));
		logger.debug("Local authentication challenge in base64: " + localChallenge);
		
	}
	
	/**
	 * Sets a specified user for decryption and encryption (for loading keys mainly)
	 * @param user 
	 * @param rsasPrivateKeyPassword
	 */
	public boolean setUser(String user, String rsaPrivateKeyPassword) {
		return this.rsaChannel.loadUserKeys(user, rsaPrivateKeyPassword);
	}
	
	/**
	 * Sets the remote user name, to use his public rsa key for encryption
	 * @param user 
	 */
	public boolean setRemoteUser(String user) {
		return this.rsaChannel.loadRemoteUserPublicKey(user);
	}

	@Override
	public String readLine() throws IOException {
		if (pendingList) {
			pendingList = false;
			appendToInputStream("!logout");
			return "!list";
		}
		
		String line = "";
		do {
		line = channel.readLine();
		logger.debug("Receiving Message: " + line);
		if (line == null) throw new NullPointerException();
		String[] splitLine = line.split(" ");
		// receiving message #1 - happens on server side
		if (splitLine[0].equals("!login") && splitLine.length >= 4) {
			loginMessage = line;
			loginName = splitLine[1];
			boolean remoteUserFound = setRemoteUser(loginName);
			remoteChallenge = splitLine[3];
			logger.debug("Remote client authentication challenge in base64: " + remoteChallenge);
			String returnMessage = "!ok " + remoteChallenge + " " + localChallenge + " abcde edcba";
			if (remoteUserFound) {
				this.waitingForMessage = 3;
				channel.println(returnMessage);
				channel.flush();
				logger.debug("Sending Login Message #2: " + returnMessage);
			} else logger.error("Remote user unknown");
		}
		
		// receiving message #2 - happens on client side
		else if (this.waitingForMessage == 2 && splitLine[0].equals("!ok")  && splitLine.length >= 5) {
			// Check if message from server contains local challenge
			if (splitLine[1].equals(localChallenge)) {
				this.remoteChallenge = splitLine[2];
				channel.println(remoteChallenge);
				channel.flush();
				logger.debug("Sending Login Message #3: " + remoteChallenge);
				useAESChannel();
				waitingForMessage = 0;
				authorized = true;
				if (!loginName.equals(systemName)) System.out.println(loginName + " has been successfully authorized");
				return this.readLine();
			} else {
				logger.error("Responded challenge from server: " + splitLine[1] + " doesn't match client challenge: " + localChallenge);
				return this.readLine();
			}
		}
		
		// receiving message #3 - happens on server side
		else if (waitingForMessage == 3) {
			if (splitLine[0].equals(localChallenge)) {
				authorized = true;
				useAESChannel();
				waitingForMessage = 0;
				if (loginName.equals(systemName)) pendingList = true;
				return loginMessage;
			} else {
				logger.error("Responded challenge from client: " + splitLine[0] + " doesn't match server challenge: " + localChallenge);
			}
		} 
		
		// logout on server side
		else if (splitLine.length >= 1 && splitLine[0].equals("!logout")) {
//			channel = rsaChannel;
//			logger.debug("Changing channel encryption to RSA");
//			authorized = false;
//			waitingForMessage = 1;
			reset();
			return line;
		}
		
		} while (waitingForMessage != 0);
		return line;
	}

	@Override
	public void close() {
		channel.close();
	}

	@Override
	public void flush() {
		channel.flush();
	}

	@Override
	public void println(String line) {
		String[] splitLine = line.split(" ");
		if (splitLine.length >= 1) {
			// on client side
			if (splitLine.length >= 2 && splitLine[0].equals("!login") && !authorized) {
				line = line + " " + localChallenge;
				loginName = splitLine[1];
				setRemoteUser("auction-server");
				System.out.println("Enter pass phrase for RSA Private key:");
				try {
					String password = (new BufferedReader(new InputStreamReader(System.in)).readLine());
					boolean userFound = setUser(loginName, password);
					if (userFound) {
						this.waitingForMessage = 2;
						logger.debug("Sending Login message #1: " + line);
						channel.println(line);
						channel.flush();
					}
				} catch (IOException ex) {
					//logger.error("One");
					ex.printStackTrace();
				}
				
			} else if (splitLine[0].equals("!list") && !authorized) {
				boolean userFound = setUser(systemName, "12345");
				setRemoteUser("auction-server");
				
				if (userFound) {
				channel.println("!login " + systemName + " " + socket.getLocalPort() + " " + localChallenge);
				channel.flush();
				this.waitingForMessage = 2;
				}
				//return;
			} else if (splitLine[0].equals("!logout")) {
//				authorized = false;
//				channel = rsaChannel;
//				waitingForMessage = 1;
				aesChannel.println(line);
				aesChannel.flush();
				reset();
			}
		}
		if (authorized) {
			channel.println(line);
			channel.flush();
		}
	}
	
	private void useAESChannel() {
		this.channel = this.aesChannel;
		logger.info("Changing channel encryption to AES");
	}

	@Override
	public void appendToInputStream(String line) {
		channel.appendToInputStream(line);
	}
	
}
