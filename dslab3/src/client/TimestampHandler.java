package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import security.RSAChannel;

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
		logger.debug("TimestampHandler startet on socket "+ socket+" to create a signed timestamp");
		// communicate
		String inputLine, outputLine;
		
		try {
			out = new PrintWriter(socket.getOutputStream());
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			logger.error("Failed to bind input or output stream to user client");
		}
		try {
			// read line and pass it to the CommunicationProtocoll
			while ((inputLine = in.readLine()) != null) {
				logger.debug("Receiving client command in timestampHandler: " + inputLine);
				outputLine = processInput(inputLine);
				out.println(outputLine);
				logger.debug("Sending client response in timestampHandler: " + outputLine);
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
		String tmpAnswer = null;
		String answer = null;
		
		if (inputLine.startsWith("!getTimestamp")) {
			String[] splitInput = inputLine.split(" ");
			if (splitInput.length < 3) {
				logger.error("got wrong formated timestamp- request");
				return "wrong request format";
			} else {
				tmpAnswer = "!timestamp " + splitInput[1] + " " + splitInput[2]  + " " + Long.toString(System.currentTimeMillis());
				
				// sign the answer
				Signature signature = null;
				try {
					signature = Signature.getInstance("SHA512withRSA");
				} catch (NoSuchAlgorithmException e) {
					logger.error("didn't find the algorithm for signature");
				}
				/* Initializing the object with a private key */
				PrivateKey privateKey = RSAChannel.getPrivateKey();
				
				try {
					signature.initSign(privateKey);
					/* Update and sign the data */
					signature.update(tmpAnswer.getBytes());
					
					byte[] signatureArray = signature.sign();
					tmpAnswer = tmpAnswer + " " + new String(Base64.encode(signatureArray));
				} catch (InvalidKeyException e) {
					logger.error("got invalid key");
				} catch (SignatureException e) {
					logger.error("wrong signatur format");
				}
				answer = tmpAnswer;
			}
		}
		return answer;
	}

	public void shutdown() {
		try {
			in.close();
			socket.close();
		} catch (IOException e) {
			logger.error("error while closing");
		}
		out.close();
	}
}
