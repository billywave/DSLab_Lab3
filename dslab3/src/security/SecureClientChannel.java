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
public class SecureClientChannel implements Channel {
	private Logger logger = Logger.getLogger(this.getClass());
	private final String B64 = "a-zA-Z0-9/+";
	
	private Channel channel;
	private RSAChannel rsaChannel;
	private AESChannel aesChannel;
	private TCPChannel tcpChannel;
	private final Socket socket;
	
	private String localChallengeB64;
	private String remoteChallengeB64;
	
	private String loginName = "";
	private boolean authorized = false;

	public SecureClientChannel(Socket socket) {
		this.socket = socket;
		
		this.tcpChannel = new TCPChannel(socket);
		Base64Channel base64Channel = new Base64Channel(tcpChannel);
		this.rsaChannel = new RSAChannel(base64Channel);
		this.aesChannel = new AESChannel(base64Channel);
		channel = rsaChannel;
		
		// generates a 32 byte secure random number
		SecureRandom secureRandom = new SecureRandom();
		final byte[] number = new byte[32];
		secureRandom.nextBytes(number);
		localChallengeB64 = new String(Base64.encode(number));
		logger.debug("Local authentication challenge in base64: " + localChallengeB64);
		
		
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
		String line = channel.readLine();
		logger.debug("SCC: Receiving Message: " + line);
		if (line == null) throw new NullPointerException();
		String[] splitLine = line.split(" ");
		
		// receiving message #2
		if (!authorized && splitLine[0].equals("!ok")  && splitLine.length >= 5) {
			try {
				assert line.matches("!ok ["+B64+"]{43}= ["+B64+"]{43}= ["+B64+"]{43}= ["+B64+"]{22}==") : "2nd message";
			
				// Check if message from server contains local challenge
				if (splitLine[1].equals(localChallengeB64)) {
					this.remoteChallengeB64 = splitLine[2];
					aesChannel.setSecretKey(splitLine[3]);
					aesChannel.setIV(splitLine[4]);
					logger.debug("Changing channel to AES");
					channel = aesChannel;

					channel.println(remoteChallengeB64);
					channel.flush();
					logger.debug("Sending Login Message #3: " + remoteChallengeB64);
					authorized = true;
					System.out.println(loginName + " has been successfully authorized");
					return this.readLine();
				} else {
					logger.error("Responded challenge from server: " + splitLine[1] + " doesn't match client challenge: " + localChallengeB64);
					return this.readLine();
				}
			} catch (AssertionError e) {
				logger.error("Assertion Error: " + e.getMessage());
			}
		}

	logger.debug("Returning message: " + line);
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
		if (splitLine.length >= 2 && splitLine[0].equals("!login")) {
			if (!authorized) {
				channel = rsaChannel;
				rsaChannel.setEncryptedRead(true);
				aesChannel.encryptedRead(true);
				line = line + " " + localChallengeB64;
				loginName = splitLine[1];
				setRemoteUser("auction-server");

				System.out.println("Enter pass phrase for RSA Private key:");
				try {
					String password = (new BufferedReader(new InputStreamReader(System.in)).readLine());
					boolean userFound = setUser(loginName, password);
					if (userFound) {
						logger.debug("Sending Login message #1: " + line);
						channel.println(line);
						channel.flush();
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}

			} else System.out.println("Please log out first, you are currently logged in as: " + loginName);
		} else if (splitLine.length == 1 && splitLine[0].equals("!list") && !authorized) {
			rsaChannel.setEncryptedRead(false);
			tcpChannel.println("!list");
			tcpChannel.flush();
		} else if (splitLine.length >= 1 && splitLine[0].equals("!logout")) {
			authorized = false;
			channel = rsaChannel;
			loginName = "";
			rsaChannel.setEncryptedRead(false);
			aesChannel.encryptedRead(false);
			aesChannel.println(line);
			aesChannel.flush();
		} else {

			// needs to be authorized or RSA key wouldn't match
			if (authorized) {
				logger.debug("Secure: Sending client response: " + line);
				channel.println(line);
				channel.flush();
			}
		}
	}
}
