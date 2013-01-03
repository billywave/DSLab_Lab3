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
	
	private Channel readChannel;
	private Channel printChannel;
	private RSAChannel rsaChannel;
	private AESChannel aesChannel;
	private TCPChannel tcpChannel;
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
		localChallenge = new String(Base64.encode(number));
		logger.debug("Local authentication challenge in base64: " + localChallenge);
		
		
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
		line = readChannel.readLine();
		if (line.equals("!list")) return line;
		logger.debug("Receiving Message: " + line);
//		if (line == null) throw new NullPointerException();
		String[] splitLine = line.split(" ");
		// receiving message #1
		if (splitLine.length >= 4 && splitLine[0].equals("!login") && !authorized) {
			printChannel = rsaChannel;
			loginMessage = line;
			loginName = splitLine[1];
			boolean remoteUserFound = setRemoteUser(loginName);
			remoteChallenge = splitLine[3];
			logger.debug("Remote client authentication challenge in base64: " + remoteChallenge);
			String aesSecretKey = aesChannel.generateBase64SecretKey();
			String aesIv = aesChannel.generateBase64IV();
			aesChannel.setSecretKey(aesSecretKey);
			aesChannel.setIV(aesIv);
			String returnMessage = "!ok " + remoteChallenge + " " + localChallenge + " " + aesSecretKey + " " + aesIv;
			if (remoteUserFound) {
//				this.waitingForMessage = 3;
				readChannel.println(returnMessage);
				readChannel.flush();
				logger.debug("Sending Login Message #2: " + returnMessage);
			} else logger.error("Remote user unknown");
		}
		
		// receiving message #3
		else if (!authorized) {
			if (splitLine[0].equals(localChallenge)) {
				authorized = true;
				readChannel = aesChannel;
				printChannel = aesChannel;
//				waitingForMessage = 0;
//				if (loginName.equals(systemName)) {
//					//pendingList = true;
//					//aesChannel.appendToInputStream("!logout");
//					aesChannel.appendToInputStream("!list");
//				}
				return loginMessage;
			} else {
				logger.error("Responded challenge from client: " + splitLine[0] + " doesn't match server challenge: " + localChallenge);
			}
		} 
		
		// logout
		else if (splitLine.length >= 1 && splitLine[0].equals("!logout")) {
			readChannel = rsaChannel;
			printChannel = tcpChannel;
			logger.debug("Changing channel encryption to RSA");
//			authorized = false;
//			waitingForMessage = 1;
			authorized = false;
			//pendingList = false;
			loginMessage = "";
//			waitingForMessage = 1;
			loginName = "";
			//this.readChannel = rsaChannel;
			aesChannel.println("Changing server channel to RSA");
			aesChannel.flush();
			return line;
		}
		
		} while (!authorized);
		logger.debug("Returning message: " + line);
//		if (!(line.equals("!list") || line.equals("!logout")) && !readingAllowed) return "System not authorized";
		return line;
		//return this.readLine();
		
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
	
	@Override
	public void appendToInputStream(String line) {
		readChannel.appendToInputStream(line);
	}
	
}
