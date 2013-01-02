package auctionServer;

import java.net.InetAddress;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.sql.Timestamp;

import org.apache.log4j.Logger;

import rmi_Interfaces.BillingServerSecure_RO;
import rmi_Interfaces.BillingServer_RO;
import rmi_Interfaces.MClientHandler_RO;
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
		if (cmdPart.equals("!getClientList")) {
			return getClientList();
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
			logger.info("User " + parameterPart + " logged in");
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
	
	/**
	 * return a list of all online clients
	 * 
	 * @return
	 */
	private String getClientList() {
		return userManagement.getAllUsers();
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
         * Authentication of the auction server on the billing server
         * Obtains a remote RMI handler for the secure billing server methods
         * unless login fails
         */
        private void billingServerLogin() {
            if (billingServerHandler != null) {
                try {
                    billingServerSecureHandler = billingServerHandler.login("auctionserver", "supersecure");
                    if (billingServerSecureHandler == null) {
                        logger.error("Login to billing server failed");
                    } else logger.debug("Login to billing server");
                } catch (RemoteException ex) {
                    logger.error("Billing Server login Remote Exception");
                }
            } else {
                logger.error("Not connected to the billing server"); // log
            }
        }
        
        /**
         * Called by the auction server, when an auction ends to add it to the user's bill
         * @param user Name of the user who won the auction as a string
         * @param auctionID Unique auction id
         * @param price Winning price of the auction
         */
        protected static void billAuction(String user, int auctionID, double price) {
            if (billingServerSecureHandler != null) {
                try {
                billingServerSecureHandler.billAuction(user, auctionID, price);
                } catch (RemoteException re) {
                    logger.error("Billing Server Connection failed: Remote Exception");
                }
            } else logger.warn("Failed to bill auction: No connection to the billing server");
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
