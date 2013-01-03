package security;

import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

/**
 *
 * @author Bernhard
 */
public class SecureServerChannel implements Channel {
	private Logger logger = Logger.getLogger(this.getClass());
	private final String B64 = "a-zA-Z0-9/+";

	
	private Channel readChannel;
	private Channel printChannel;
	private RSAChannel rsaChannel;
	private AESChannel aesChannel;
	private TCPChannel tcpChannel;
	private final Socket socket;
	
	private String localChallengeB64;
	private String remoteChallengeB64;

	private String loginMessage = "";

	private String loginName;
	private boolean authorized = false;

	public SecureServerChannel(Socket socket) {
		this.socket = socket;

		tcpChannel = new TCPChannel(socket);
		Base64Channel base64Channel = new Base64Channel(tcpChannel);
		this.rsaChannel = new RSAChannel(base64Channel);
		this.aesChannel = new AESChannel(base64Channel);
		this.readChannel = rsaChannel;
		this.printChannel = tcpChannel;
		
		setUser("auction-server", "23456");
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
		String line;
		do {
			line = readChannel.readLine();
			if (line == null) {
				logger.debug("Reading failed: received null");
				return null;
			} else if (line.equals("!list")) return line;
			else {
				logger.debug("Receiving Message: " + line);
			

				String[] splitLine = line.split(" ");
				// receiving message #1
				if (splitLine.length >= 4 && splitLine[0].equals("!login") && !authorized) {
					try {
						assert line.matches("!login [a-zA-Z0-9_\\-]+ [0-9]+ ["+B64+"]{43}=") : "1st message";
						printChannel = rsaChannel;
						loginMessage = line;
						loginName = splitLine[1];
						boolean remoteUserFound = setRemoteUser(loginName);
						remoteChallengeB64 = splitLine[3];
						logger.debug("Remote client authentication challenge in base64: " + remoteChallengeB64);
						String aesSecretKeyB64 = aesChannel.generateBase64SecretKey();
						String aesIvB64 = aesChannel.generateBase64IV();
						aesChannel.setSecretKey(aesSecretKeyB64);
						aesChannel.setIV(aesIvB64);
						String returnMessage = "!ok " + remoteChallengeB64 + " " + localChallengeB64 + " " + aesSecretKeyB64 + " " + aesIvB64;
						if (remoteUserFound) {
							readChannel.println(returnMessage);
							readChannel.flush();
							logger.debug("Sending Login Message #2: " + returnMessage);
							logger.debug("Changing channel to AES");
							readChannel = aesChannel;
							printChannel = aesChannel;
						} else logger.error("Remote user unknown");
					} catch (AssertionError e) {
						logger.error("Assertion Error: "+e.getMessage());
					}

				}

				// receiving message #3
				else if (!authorized) {
					try {
						assert line.matches("["+B64+"]{43}=") : "3rd message";
						if (splitLine.length >= 1 && splitLine[0].equals(localChallengeB64)) {
							authorized = true;
							return loginMessage;
						} else {
							logger.error("Responded challenge from client: " + splitLine[0] + " doesn't match server challenge: " + localChallengeB64);
						}
					} catch (AssertionError e) {
						logger.error("Assertion error: "+e.getMessage());
					}
				} 

				// logout
				else if (splitLine.length >= 1 && splitLine[0].equals("!logout")) {
					readChannel = rsaChannel;
					printChannel = tcpChannel;
					logger.debug("Changing channel encryption to RSA");
					authorized = false;
					loginMessage = "";
					loginName = "";
					aesChannel.println("Changing server channel to RSA");
					aesChannel.flush();
					return line;
				}	
			}
		} while (!authorized);

		logger.debug("Returning message: " + line);
		return line;
	}

	@Override
	public void close() {
		readChannel.close();
	}

	@Override
	public void flush() {
		readChannel.flush();
	}

	@Override
	public void println(String line) {
		logger.debug("SSC: Sending response to client: " + line);
		printChannel.println(line);
		printChannel.flush();
	}
}
