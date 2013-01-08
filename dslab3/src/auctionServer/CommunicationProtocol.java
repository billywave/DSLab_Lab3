package auctionServer;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.sql.Timestamp;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.bouncycastle.openssl.PEMReader;

import rmi_Interfaces.BillingServerSecure_RO;
import rmi_Interfaces.BillingServer_RO;
import rmi_Interfaces.MClientHandler_RO;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;

import event.AuctionEvent;
import event.UserEvent;
import exceptions.WrongEventTypeException;

/**
 * protocoll for interprating the clients Message
 * 
 * @author Alexander Tatowsky
 *
 */
public class CommunicationProtocol {
	
	private static Logger logger = Logger.getLogger(CommunicationProtocol.class);
	 
	User user = new User("",false,null,0,0);
	
	String[] stringParts;
	String cmdPart = "";
	String parameterPart = "";
	String answer = "";
	
	InetAddress inetAddress = null;
	int tcpPort;
	int udpPort;
	
	// for RMI
	Registry registry;
	String registryHost;
	int registryPort;
	String analyticsServerRef;
	String billingServerRef;
	MClientHandler_RO mClientHandler = null;
        BillingServer_RO billingServerHandler = null;
        static BillingServerSecure_RO billingServerSecureHandler = null;
	
	UserManagement userManagement;
	
	public CommunicationProtocol(InetAddress inetAddress, int port, UserManagement userManagement, String analyticsServerRef, String billingServerRef, Registry registry) {
		this.inetAddress = inetAddress;
		this.tcpPort = port;
		this.userManagement = userManagement;
		this.analyticsServerRef = analyticsServerRef;
		this.billingServerRef = billingServerRef;
		this.registry = registry;
		
		try {
			mClientHandler = (MClientHandler_RO) registry.lookup(analyticsServerRef);
		} catch (AccessException e1) {
			logger.error("Access to the registry denied");
		} catch (RemoteException e1) {
			logger.error("Failed to connect to the Analytics Server");
		} catch (NotBoundException e1) {
			logger.error("Analytics Server not bound to the registry");
		}
                
		try {
			billingServerHandler = (BillingServer_RO) registry.lookup(billingServerRef);
			this.billingServerLogin();
		} catch (AccessException e1) {
			logger.error("Access to the registry denied");
		} catch (RemoteException e1) {
			System.out.println("Failed to connect to the Billing Server");
		} catch (NotBoundException e1) {
			logger.error("Billing Server not bound. Ref:" + billingServerRef);
		}
	}

	/**
	 * interprate input String and call the right methods
	 * 
	 * @param input
	 * @return
	 */
	public String processInput(String input) {
		
		stringParts = input.split(" ");
		
		try {
			cmdPart = stringParts[0];
		} catch (NullPointerException e) {
			System.out.println("Error: found no Command");
		}
		
		if (cmdPart.equals("!login")) {
			return logginUser();
		}
		if (cmdPart.equals("!logout")) {
			return loggoutUser();
		}
		if (cmdPart.equals("!list")) {
			return listAuctions();
		}
		if (cmdPart.equals("!create")) {
			return createAuction();
		}
		if (cmdPart.equals("!bid")) {
			return bidForAuction();
		}
		if (cmdPart.equals("!groupBid")) {
			return groupBidForAuction();
		}
		if (cmdPart.equals("!confirm")) {
			return confirmGroupBid();
		}
		if (cmdPart.equals("!getClientList")) {
			return getClientList();
		}
		if (cmdPart.equals("!getFirstClientList")) {
			logger.debug("!getFirstClientList - command set");
			return getFirstClientList();
		}
		if (cmdPart.equals("!signedBid")) {
			return determineSignedBid();
		}
		return "Error: Unknown command: " + input;
	}
	

	/**
	 * call the UserManagements method to logg user in. 
	 * 
	 * @return answer as in assignement
	 */
	private String logginUser() {
		try {
			parameterPart = stringParts[1];
			udpPort = Integer.parseInt(stringParts[2]);
		} catch (ArrayIndexOutOfBoundsException e) {
			return answer = "Error: Please enter the log in command like this: !login <Username>";
		} catch (NumberFormatException e) {
			return answer = "Error: Please enter the log in command like this: !login <Username>";
		}
		
		if (user.isOnline()) {
			return "Error: You have to log out first!";
		}
		// login successfully -> throw USER_LOGIN- Event
		if ((user = userManagement.logginUser(parameterPart, inetAddress, tcpPort, udpPort)) != null) {
			Timestamp logoutTimestamp = new Timestamp(System.currentTimeMillis());
			long timestamp = logoutTimestamp.getTime();
			try {
				mClientHandler.processEvent(new UserEvent(UserEvent.USER_LOGIN, timestamp, parameterPart));
			} catch (RemoteException e) {
				logger.error("Failed to connect to the Analytics Server");
			} catch (WrongEventTypeException e) {
				logger.error("Wront type of Event");
			} catch (NullPointerException e) {
				logger.error("Failed to connect to the Analytics Server");
			}
			logger.info("User " + parameterPart + " logged in with Port: " + udpPort);
			return answer = "Successfully logged in as " + parameterPart;
		} else {
			user = new User("",false,null,0,0);
			return answer = "Error: User is already logged in!";
		}
	}
	
	/**
	 * call the UserManagements method
	 * 
	 * @return answer as in assignement
	 */
	private String loggoutUser() {

		if (!user.isOnline()) {
			return "Error: You have to log in first";
		}
		else {
			if (userManagement.loggoutUser(user.getName(), inetAddress, tcpPort)) {
				Timestamp logoutTimestamp = new Timestamp(System.currentTimeMillis());
				long timestamp = logoutTimestamp.getTime();
				try {
					mClientHandler.processEvent(new UserEvent(UserEvent.USER_LOGOUT, timestamp, parameterPart));
				} catch (RemoteException e) {
					logger.error("Failed to connect to the Analytics Server");
				} catch (NullPointerException e) {
					//logger.error("Failed to connect to the Analytics Server");
				} catch (WrongEventTypeException e) {
					logger.error("Wrong type of Event");
				}
				return "Successfully logged out as " + parameterPart + "!";
			}
			else {
				return "Error: You can only loggout yourself!";
			}
			
		}
	}

	/**
	 * call the UserManagements method
	 * 
	 * @return answer as in assignement
	 */
	private String createAuction() {
		if (user.isOnline()) {
			int duration = 0;
			String describtion = "";
			
			try {
				duration = Integer.parseInt(stringParts[1]);
				
				if (duration <= 0) {
					return "Error: The duration has to be > 0!";
				}
				
				for (int i = 2; i < stringParts.length; i++) {
					describtion += " " + stringParts[i];
				}
				describtion = describtion.substring(1);
			} catch (ArrayIndexOutOfBoundsException e) {
				return answer = "Error: Please enter the create command like this: " +
						"!create <duration> + <describtion>";
			} catch (NumberFormatException e) {
				return answer = "Error: The Server does not allow this time- format!";
			}
			
			Auction auction = new Auction(user, duration, describtion, udpPort, userManagement, mClientHandler);
			auction = userManagement.createAuction(auction);
			
			// RMI AUCTION_STARTED
			Timestamp logoutTimestamp = new Timestamp(System.currentTimeMillis());
			long timestamp = logoutTimestamp.getTime();
			try {
				mClientHandler.processEvent(new AuctionEvent(AuctionEvent.AUCTION_STARTED, timestamp, auction.getId()));
			} catch (RemoteException e) {
				logger.error("Failed to connect to the Analytics Server");
			} catch (WrongEventTypeException e) {
				logger.error("Wrong type of Event");
			} catch (NullPointerException e) {
				
			}
			
			String response = "An auction '" + auction.describtion + 
				"' with id " + auction.getId() + 
				" has been created and will end on " + 
				auction.getEndOfAuctionTimestamp() + " CET.";
			logger.info(response);
			return response;
		}
		return "You have to log in first!";
	}
	
	/**
	 * call the UserManagements method
	 * 
	 * @return answer as in assignement
	 */
	private String listAuctions() {
		String auctions = "";
		auctions = userManagement.getAuctions();
		return auctions;
	}
	
	/**
	 * call the UserManagements method
	 * 
	 * @return answer as in assignement
	 */
	private String bidForAuction() {
		if (user.isOnline()) {
			int auctionID = 0;
			double amount = 0.0;
			
			if (stringParts.length < 3) {
				return "Error: Please enter the bid- command like this: " +
						"!bid <auction-id> <amount>";
			}
			try {
				auctionID = Integer.parseInt(stringParts[1]);
				amount = Double.parseDouble(stringParts[2]);
				amount = (double)(Math.round(amount*100))/100;
				
				if (amount <= 0 ) {
					return "Error: The amount has to be > 0!";
				}
			} catch (NumberFormatException e) {
				return "Error: Please enter the bid- command like this: " +
						"!bid <auction-id> <amount>";
			} catch (ArrayIndexOutOfBoundsException e) {
				return "Error: Please enter the bid- command like this: " +
						"!bid <auction-id> <amount>";
			}
			
			return userManagement.bidForAuction(user, auctionID, amount);
		}
		return "You have to log in first!";
	}
	
	private String groupBidForAuction() {
		if (user.isOnline()) {
			int auctionID = 0;
			double amount = 0.0;
			
			if (stringParts.length < 3) {
				return "Error: Please enter the bid- command like this: " +
						"!groupBid <auction-id> <amount>";
			}
			try {
				auctionID = Integer.parseInt(stringParts[1]);
				amount = Double.parseDouble(stringParts[2]);
				amount = (double)(Math.round(amount*100))/100;
				
				if (amount <= 0 ) {
					return "Error: The amount has to be > 0!";
				}
			} catch (NumberFormatException e) {
				return "Error: Please enter the bid- command like this: " +
						"!groupBid <auction-id> <amount>";
			} catch (ArrayIndexOutOfBoundsException e) {
				return "Error: Please enter the bid- command like this: " +
						"!groupBid <auction-id> <amount>";
			}
			
			return userManagement.groupBidForAuction(auctionID, amount, user);
		}
		return "You have to log in first!";
	}
	
	private String confirmGroupBid() {
		if (user.isOnline()) {
			int auctionID = 0;
			double amount = 0.0;
			User biddingUser;
			
			if (stringParts.length < 3) {
				return "Error: Please enter the bid- command like this: " +
						"!groupBid <auction-id> <amount>";
			}
			try {
				auctionID = Integer.parseInt(stringParts[1]);
				amount = Double.parseDouble(stringParts[2]);
				amount = (double)(Math.round(amount*100))/100;
				String userName = stringParts[3];
				
				biddingUser = userManagement.getUserByName(userName);
				if (biddingUser == null) return "!rejected User not found";
				
				if (amount <= 0 ) {
					return "!rejected The amount has to be > 0!";
				}
			} catch (NumberFormatException e) {
				return "!rejected Please enter the bid- command like this: " +
						"!confirm <auction-id> <amount> <username>";
			} catch (ArrayIndexOutOfBoundsException e) {
				return "!rejected Please enter the bid- command like this: " +
						"!confirm <auction-id> <amount> <username>";
			}
			
			return userManagement.confirmGroupBid(auctionID, amount, biddingUser, user);
		}
		return "!rejected You have to log in first!";
	}
	
	/**
	 * return a list of all online clients
	 * 
	 * @return
	 */
	private String getClientList() {
		return "users: " + userManagement.getAllUsers();
	}
	
	private String getFirstClientList() {
		String allUsers = "firstusers: " + userManagement.getAllUsers();
		logger.debug("sending: " + allUsers);
		return allUsers;
	}
	
	public boolean isOnline() {
		if (user.isOnline()) {
			return true;
		}
		else {
			return false;
		}
	}

	public User getUser() {
		return user;
	}

	/**
	 * Authentication of the auction server on the billing server Obtains a
	 * remote RMI handler for the secure billing server methods unless login
	 * fails
	 */
	private void billingServerLogin() {
		if (billingServerHandler != null) {
			try {
				billingServerSecureHandler = billingServerHandler.login(
						"auctionserver", "supersecure");
				if (billingServerSecureHandler == null) {
					logger.error("Login to billing server failed");
				} else
					logger.debug("Login to billing server");
			} catch (RemoteException ex) {
				logger.error("Billing Server login Remote Exception");
			}
		} else {
			logger.error("Not connected to the billing server"); // log
		}
	}

	/**
	 * Called by the auction server, when an auction ends to add it to the
	 * user's bill
	 * 
	 * @param user
	 *            Name of the user who won the auction as a string
	 * @param auctionID
	 *            Unique auction id
	 * @param price
	 *            Winning price of the auction
	 */
	protected static void billAuction(String user, int auctionID, double price) {
		if (billingServerSecureHandler != null) {
			try {
				billingServerSecureHandler.billAuction(user, auctionID, price);
			} catch (RemoteException re) {
				logger.error("Billing Server Connection failed: Remote Exception");
			}
		} else
			logger.warn("Failed to bill auction: No connection to the billing server");
	}

	/**
	 * Receive a signed bid because the server went offline and online again
	 * the signed bid should be formated like this:
	 * !signedBid 17 90 Bob:<timestamp1>:<signature1> Carl:<timestamp2>:<signature2>
	 * 
	 * then do the following things:
	 * 1) recreate the two !timestamp <auctionID> <price> <timestamp> statements and test them 
	 * against the signature.
	 * 
	 * 2) if they match determine the arithmetic mean of the two timestamps and create a bid
	 * 
	 * 3) if the bid has been closed while the server was offline decide who is the winner in 
	 *    compliance on whether the bid was created until the auction would have been online or not
	 * 
	 * @return
	 */
	private String determineSignedBid() {
		String[] timestampPart1 = stringParts[3].split(":");
		String[] timestampPart2 = stringParts[4].split(":");
		
		String signature1Base64 = timestampPart1[2];
		String signature2Base64 = timestampPart2[2];
		
		String signer1Name = timestampPart1[0];
		String signer2Name = timestampPart2[0];
		
		String signedMessage1 = "!timestamp " + stringParts[1] + " "
				+ stringParts[2] + " " + timestampPart1[1];

		String signedMessage2 = "!timestamp " + stringParts[1] + " "
				+ stringParts[2] + " " + timestampPart2[1];
		
		boolean verifySignature1 = verifySignedMessage(signedMessage1, signer1Name, signature1Base64);
		boolean verifySignature2 = verifySignedMessage(signedMessage2, signer2Name, signature2Base64);
		
		if (verifySignature1 && verifySignature2) {
			logger.info("Verifikation of the bid was successfull!");
			
			int auctionID = Integer.parseInt(stringParts[1]);
			double price = Double.parseDouble(stringParts[2]);
			price = (double)(Math.round(price*100))/100;
			
			if (price <= 0 ) {
				return "Error: The amount has to be > 0!";
			}
			signedBidForAuctions(auctionID, price, new Timestamp(Long.parseLong(timestampPart1[1])));
			
			return "Verifikation of the bid was successfull!";
		}
		
		return "Error: The signature of the bid could not be verified!";
	}

	/**
	 * actual verification of the signature
	 * 
	 * @param message
	 * @param signerName
	 * @param signature_Base64encoded
	 * @return true if the signature is correct
	 */
	private boolean verifySignedMessage(String message, String signerName, String signature_Base64encoded) {
		byte[] signature1Decoded = null;
		
		try {
			signature1Decoded = Base64.decode(signature_Base64encoded);
		} catch (Base64DecodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Signature signature = null;
		try {
			signature = Signature.getInstance("SHA512withRSA");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/* Initializing the object with the public key */
		PublicKey publicKey = readPublicKey(signerName);
		try {
			signature.initVerify(publicKey);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/* Update and verify the data */
		boolean verifies = false;
		try {
			signature.update(message.getBytes());
			verifies = signature.verify(signature1Decoded);
			logger.info("signature from " + signerName + " verifies: " + verifies);
			if (!verifies) logger.debug("message: " + message);
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return verifies;
	}
	
	/**
	 * work of missed bids.
	 * therefore search for the highest amount in the valid timelaps
	 * 
	 * @param auctionID
	 * @param price
	 * @param timestamp
	 * @return
	 */
	private String signedBidForAuctions(int auctionID, double price, Timestamp timestamp) {
		
		Iterator<Auction> iterator = userManagement.syncAuctionList.iterator();
		while (iterator.hasNext()) {
			Auction auction = iterator.next();
			if (auction.getId() == auctionID) {
				// TODO Alex: new funktion: userManagement.signedBidForAuciton
				
			}
		}
		return null;
	}
	
	/**
	 * Reads the public key of a given user
	 * @param user 
	 */
	public static PublicKey readPublicKey(String user) {
		try {
			logger.debug("reading public key from user: " + user);
			PEMReader in = new PEMReader(new FileReader("keys/" + user + ".pub.pem"));
			return (PublicKey) in.readObject();
		} catch (FileNotFoundException e) {
			logger.error("Public Key File Not Found");
		} catch (IOException ex) {}
		return null;
	}
	
	public void shutdown() {
		/*
    	try {
			registry.unbind(analyticsServerRef);
		} catch (AccessException e) {
			logger.error("couldn't unbind the Registry");
		} catch (RemoteException e) {
			logger.error("couldn't unbind the Registry");
		} catch (NotBoundException e) {
			logger.error("couldn't unbind the Registry");
		}
		* */
	}

}
