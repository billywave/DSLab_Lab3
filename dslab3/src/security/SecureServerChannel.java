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
public class SecureServerChannel implements Channel {
	private Logger logger = Logger.getLogger(this.getClass());
	
	private Channel channel;
	private RSAChannel rsaChannel;
	private AESChannel aesChannel;
	private final Socket socket;
//	
	private String localChallenge;
	private String remoteChallenge;
//	
	private String loginMessage;
//	private int waitingForMessage;
//	
	private final String systemName = "system";
	private String loginName;
	private boolean authorized;
//	private boolean pendingList;
//	private boolean readingAllowed = false;

	public SecureServerChannel(Socket socket) {
		this.socket = socket;
		
//		authorized = false;
//		//pendingList = false;
		loginMessage = "";
//		waitingForMessage = 1;
//		loginName = "";
//		
		Base64Channel base64Channel = new Base64Channel(new TCPChannel(socket));
		this.rsaChannel = new RSAChannel(base64Channel);
		this.aesChannel = new AESChannel(base64Channel);
		this.channel = rsaChannel;
		
		setUser("auction-server", "23456");
		// generates a 32 byte secure random number
		SecureRandom secureRandom = new SecureRandom();
		final byte[] number = new byte[32];
		secureRandom.nextBytes(number);
		localChallenge = new String(Base64.encode(number));
		logger.debug("Local authentication challenge in base64: " + localChallenge);
		
		
	}
	
	/**
	 * Sets the channel back to handle logins
	 * @param mode "aes" or "rsa"
	 */
	private void reset() {
		
	}
	
	/**
	 * Sets a specified user for decryption and encryption (for loading keys mainly)
	 * @param user 
	 * @param rsasPrivateKeyPassword
	 */
	public final boolean setUser(String user, String rsaPrivateKeyPassword) {
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
//		if (pendingList) {
//			pendingList = false;
//			aesChannel.appendToInputStream("!logout");
//			return "!list";
//		}
//		
		String line = "";
		do {
		line = channel.readLine();
		logger.debug("Receiving Message: " + line);
//		if (line == null) throw new NullPointerException();
		String[] splitLine = line.split(" ");
		// receiving message #1
		if (splitLine.length >= 4 && splitLine[0].equals("!login") && !authorized) {
			loginMessage = line;
			loginName = splitLine[1];
			boolean remoteUserFound = setRemoteUser(loginName);
			remoteChallenge = splitLine[3];
			logger.debug("Remote client authentication challenge in base64: " + remoteChallenge);
			String returnMessage = "!ok " + remoteChallenge + " " + localChallenge + " abcde edcba";
			if (remoteUserFound) {
//				this.waitingForMessage = 3;
				channel.println(returnMessage);
				channel.flush();
				logger.debug("Sending Login Message #2: " + returnMessage);
			} else logger.error("Remote user unknown");
		}
		
		// receiving message #3
		else if (!authorized) {
			if (splitLine[0].equals(localChallenge)) {
				authorized = true;
				useAESChannel();
//				waitingForMessage = 0;
				if (loginName.equals(systemName)) {
					//pendingList = true;
					//aesChannel.appendToInputStream("!logout");
					aesChannel.appendToInputStream("!list");
				}
				return loginMessage;
			} else {
				logger.error("Responded challenge from client: " + splitLine[0] + " doesn't match server challenge: " + localChallenge);
			}
		} 
		
		// logout
		else if (splitLine.length >= 1 && splitLine[0].equals("!logout")) {
//			channel = rsaChannel;
			logger.debug("Changing channel encryption to RSA");
//			authorized = false;
//			waitingForMessage = 1;
			authorized = false;
			//pendingList = false;
			loginMessage = "";
//			waitingForMessage = 1;
			loginName = "";
			this.channel = rsaChannel;
			aesChannel.println("Changing server channel to RSA");
			aesChannel.flush();
			return line;
		}
		
		} while (!authorized);
		logger.debug("Returning message: " + line);
//		if (!(line.equals("!list") || line.equals("!logout")) && !readingAllowed) return "System not authorized";
		if (!loginName.equals(systemName) || line.equals("!list") || line.equals("!logout")) return line;
		return this.readLine();
		
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
		logger.debug("SSC: Sending response to client: " + line);
		channel.println(line);
		channel.flush();
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
