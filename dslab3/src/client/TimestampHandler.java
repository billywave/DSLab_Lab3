package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;

import org.apache.log4j.Logger;

import security.RSAChannel;

import com.sun.org.apache.xml.internal.security.utils.Base64;

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
		String tmpAnswer = null;
		String answer = null;
		
		if (inputLine.startsWith("!getTimestamp")) {
			String[] splitInput = inputLine.split(" ");
			if (splitInput.length < 3) {
				logger.error("got wrong formated timestamp- request");
				return "wrong request format";
			} else {
				tmpAnswer = "timestamp " + splitInput[1] + splitInput[2] + Long.toString(System.currentTimeMillis());
				
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
				PrivateKey privateKey = RSAChannel.getPrivateKey();
				
				try {
					signature.initSign(privateKey);
					/* Update and sign the data */
					signature.update(tmpAnswer.getBytes());
					byte[] signatureArray = signature.sign();
					tmpAnswer = tmpAnswer + " " + new String(Base64.encode(signatureArray));
				} catch (InvalidKeyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SignatureException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				answer = tmpAnswer;
			}
		}
		
		return answer;
	}

}
