package security;

import java.io.IOException;
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
	private final RSAChannel rsaChannel;
	private final AESChannel aesChannel;
	private Socket socket;
	
	private final String localChallenge;
	private String remoteChallenge;
	
	private String loginMessage;
	private int waitingForMessage = 1;
	
	private final String systemName = "system";
	private String loginName;
	private boolean authorized = false;
	private boolean pendingList = false;

	public SecureChannel(Socket socket) {
		this.socket = socket;
		Channel tempChannel = new Base64Channel(new TCPChannel(socket));
		this.rsaChannel = new RSAChannel(tempChannel);
		this.aesChannel = new AESChannel(tempChannel);
		this.channel = rsaChannel;
		
		// generates a 32 byte secure random number
		SecureRandom secureRandom = new SecureRandom();
		final byte[] number = new byte[32];
		secureRandom.nextBytes(number);
		localChallenge = new String(Base64.encode(number));
		logger.debug("Local authentication challenge in base64: " + localChallenge);
		
		
	}

	@Override
	public String readLine() throws IOException {
		if (pendingList) {
			pendingList = false;
			return "!list";
		}
		
		String line = "";
		do {
		line = channel.readLine();
		logger.debug("Receiving Message: " + line);
		
		String[] splitLine = line.split(" ");
		// receiving message #1 - happens on server side
		if (this.waitingForMessage == 1 && splitLine[0].equals("!login") && splitLine.length >= 4) {
			loginMessage = line;
			loginName = splitLine[1];
			remoteChallenge = splitLine[3];
			logger.debug("Remote client authentication challenge in base64: " + remoteChallenge);
			this.waitingForMessage = 3;
			String returnMessage = "!ok " + remoteChallenge + " " + localChallenge + " abcde edcba";
			channel.println(returnMessage);
			channel.flush();
			logger.debug("Sending Login Message #2: " + returnMessage);
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
				return channel.readLine();
			} else {
				logger.error("Responded challenge from server: " + splitLine[1] + " doesn't match client challenge: " + localChallenge);
				return channel.readLine();
			}
		}
		
		// receiving message #3 - happens on server side
		else if (waitingForMessage == 3) {
			if (splitLine[0].equals(localChallenge)) {
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
			channel = rsaChannel;
			authorized = false;
			waitingForMessage = 1;
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
			if (splitLine[0].equals("!login")) {
				line = line + " " + localChallenge;
				this.waitingForMessage = 2;
				logger.debug("Sending Login message #1: " + line);
			} else if (splitLine[0].equals("!list") && !authorized) {
				channel.println("!login " + systemName + " " + socket.getLocalPort() + " " + localChallenge);
				channel.flush();
				this.waitingForMessage = 2;
				return;
			} else if (splitLine[0].equals("!logout")) {
				authorized = false;
				channel = rsaChannel;
				waitingForMessage = 1;
			}
		}
		
		channel.println(line);
		channel.flush();
	}
	
	private void useAESChannel() {
		this.channel = this.aesChannel;
		logger.info("Changing channel encryption to AES");
	}
	
}