package client;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;

import org.apache.log4j.Logger;
import org.bouncycastle.openssl.EncryptionException;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;

public class TimestampHandler implements Runnable {

	private static Logger logger = Logger.getLogger(TimestampHandler.class);
	Socket socket;
	PrintWriter out;
	BufferedReader in;
	
	public TimestampHandler(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {
		// communicate
		String inputLine, outputLine;
		InetAddress inetAddress = socket.getInetAddress();
		int port = socket.getPort();
		
		try {
			out = new PrintWriter(socket.getOutputStream());
			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
		} catch (IOException e) {
			logger.error("Failed to bind input or output stream to user client");
		}
		try {
			// read line and pass it to the CommunicationProtocoll
			while ((inputLine = in.readLine()) != null) {
				logger.debug("Receiving client command in clientHandler: " + inputLine);
				outputLine = processInput(inputLine);
				out.println(outputLine);
				logger.debug("Sending client response in clientHandler: " + outputLine);
				out.flush();
			}
		} catch (NullPointerException e) {

		} catch (IOException e) {
			// empty
		}
	}
	
	/**
	 * handle timestamp Requests.
	 * incoming message tas to look like:
	 * "!getTimestamp <auctionID> <price>"
	 * 
	 * @param inputLine
	 * @return "!timestamp <auctionID> <price> <timestamp> <signature>"
	 */
	private String processInput(String inputLine) {
		String answer = "";
		
		if (inputLine.startsWith("!getTimestamp")) {
			String[] splitInput = inputLine.split(" ");
			if (splitInput.length < 3) {
				logger.error("got wrong formated timestamp- request");
				return "wrong request format";
			} else {
				answer = "timestamp " + splitInput[1] + splitInput[2] + Long.toString(System.currentTimeMillis());
				
				// sign the answer
				Signature signature = null;
				try {
					signature = Signature.getInstance("SHA512withRSA");
				} catch (NoSuchAlgorithmException e) {
					logger.error("didn't find the algorithm for signature");
				}
				/* Initializing the object with a private key */
				
				/**
				 * TODO Alex: find user and password
				 */
				PrivateKey privateKey = readPrivateKey("alice", "12345");
				
				try {
					signature.initSign(privateKey);
					/* Update and sign the data */
					signature.update(answer.getBytes());
					byte[] signatureArray = signature.sign();
				} catch (InvalidKeyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SignatureException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				answer = answer + "signature-dummy";
			}
		}
		return answer;
	}

	/**
	 * Reads the private key of the given user
	 */
	private PrivateKey readPrivateKey(final String user, final String password) {
		try {
			PEMReader in = new PEMReader(new FileReader("keys/" + user + ".pem"), new PasswordFinder() {
				@Override
				public char[] getPassword() {
					return password.toCharArray();
				}
			});
			
			KeyPair keyPair = (KeyPair) in.readObject();
			return keyPair.getPrivate(); 
			
		} catch (FileNotFoundException e) {
			logger.error("Private Key File Not Found");
			// TODO fill in
		} catch (EncryptionException e) {
			logger.error("Decryption failed, check password");
		} catch (IOException ex) {
			logger.error("Two");
			ex.printStackTrace();
		}
		return null;
	}
}
