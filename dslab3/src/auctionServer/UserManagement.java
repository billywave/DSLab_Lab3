package auctionServer;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;

import event.BidEvent;
import exceptions.WrongEventTypeException;
import org.apache.log4j.Logger;

import rmi_Interfaces.MClientHandler_RO;

public class UserManagement {
	private static Logger logger = Logger.getLogger(UserManagement.class);
	
	public static Timer timer = new Timer();
	
	List<User> syncUserList = Collections.synchronizedList(new ArrayList<User>());
	List<Auction> syncAuctionList = Collections.synchronizedList(new ArrayList<Auction>());
	
	// RMI
	MClientHandler_RO mClientHandler = null;
	
	/**
	 * loging in a user- as in put him into the threadsave list and setting him online
	 * 
	 * @param name
	 * @return true if user is not already logged in
	 */
	public User logginUser(String name, InetAddress inetAddress, int tcpPort, int udpPort) {
		User newUser;
		synchronized (syncUserList) {
			Iterator<User> iterator = syncUserList.iterator();
			
			while (iterator.hasNext()) {
				User user = iterator.next();
				if (user.getName().equals(name)) {   // user exists
					if (user.isOnline()) {
						return null;                // existing user is online
					}
					else {                           // existing user != online
						user.setOnline(true);
						user.setInternetAdress(inetAddress);
						user.setPort(tcpPort);
						user.setUdpPort(udpPort);
						
						// send notifications which the user might have been missing
						user.sendNotifications();
						
						return user;
					}
				}
			}
			newUser = new User(name, true, inetAddress, tcpPort, udpPort);
			syncUserList.add(newUser); // new user
		}
		return newUser;
	}
	
	/**
	 * return true if user was found and logged out.
	 * @param name
	 * @return
	 */
	public boolean loggoutUser(String name, InetAddress inetAddress, int port) {
		synchronized (syncUserList) {
			Iterator<User> iterator = syncUserList.iterator();
			
			while (iterator.hasNext()) {
				User user = iterator.next();
				
				// just loggout if name and inetAddress equals the logged in username and address.
				if (user.getName().equals(name) && user.getInternetAdress().equals(inetAddress) && user.getPort() == port) {
					user.setOnline(false);
					return true;
				}
			}
			return false;
		}
	}
	
	public void loggoutAll() {
		logger.debug("loggout all");
		synchronized (syncUserList) {
			Iterator<User> iterator = syncUserList.iterator();
			
			while (iterator.hasNext()) {
				User user = iterator.next();
				user.setOnline(false);
				logger.debug("logout user " + user.getName());
			}
			
		}
	}
	
	public Auction createAuction(Auction auction) {
		synchronized (syncAuctionList) {
			syncAuctionList.add(auction);
			// timer = new Timer();
			try {
				timer.schedule(auction, auction.durationSec * 1000);
			} catch (IllegalStateException e) {
				logger.warn("timer is already cancelt");
			}
			
		}
		return auction;
	}
	
	public User getUserByName(String name) {
		synchronized (syncUserList) {
			Iterator<User> iterator = syncUserList.iterator();
			
			while (iterator.hasNext()) {
				User user = iterator.next();
				
				// just loggout if name and inetAddress equals the logged in username and address.
				if (user.getName().equals(name))  {
					return user;
				}
			}
			return null;
		}
	}
	
	public String getAuctions() {
		String auctions = "";
		
		synchronized (syncAuctionList) {
			Iterator<Auction> iterator = syncAuctionList.iterator();
			
			Auction auction;
			int i = 1;
			if (!iterator.hasNext()) {
				auctions = "There are no auctions active!";
			}
			while (iterator.hasNext()) {
				auction = iterator.next();
				
				auctions += (i + ". " + " Auction ID: " + auction.getId() + 
						"\n   " + auction.getDescribtion() + " - created by: " + auction.getOwner().getName() + 
						"\n    Current highest bid: " + auction.printHighestAmount() + " by: " + auction.getHighestBidder().getName() + 
						"\n    ends at: " + auction.getEndOfAuctionTimestamp() + 
						"\n----------------------------------------------\n\n");
				i++;
			}
		}
		return auctions;
	}
	
	public String bidForAuction(User user, int auctionID, double amount) {
		String answer = "";
		
		synchronized (syncAuctionList) {
			Iterator<Auction> iterator = syncAuctionList.iterator();
			Auction auction;
			
			while (iterator.hasNext()) {
				auction = iterator.next();
				if (auction.getId() == auctionID) {
					if (amount > auction.getHightestAmount()) {
						User oldHighestBidder = auction.getHighestBidder();
						
						auction.setHightestAmount(amount);
						auction.setHighestBidder(user);

						if (!oldHighestBidder.getName().equals("none")) {
							
							//RMI- BID_PLACED
							Timestamp logoutTimestamp = new Timestamp(System.currentTimeMillis());
							long timestamp = logoutTimestamp.getTime();
							try {
								mClientHandler.processEvent(new BidEvent(BidEvent.BID_OVERBID, timestamp, user.getName(), auctionID, amount));
							} catch (RemoteException e) {
								logger.error("Failed to connect to the Analytics Server");
							} catch (WrongEventTypeException e) {
								logger.error("Wrong type of event");
							}
							
							String msg = "!new-bid " + auction.getDescribtion();
							// send UDP- notofication that he has been oberbidden if he is online
							if (oldHighestBidder.isOnline()) {
								try {
									AuctionServer_UDPSocket.getInstamce().sendMessage(oldHighestBidder.getInternetAdress(), oldHighestBidder.getUdpPort(), msg);
								} catch (IOException e) {
									System.out.println("Error: Trying to send UDP- Message to unknown user");
								}
							}
							else {
								// if he is not online store the notification
								oldHighestBidder.storeNotification(msg);
							}
						}
						else {
							//RMI- BID_PLACED
							Timestamp logoutTimestamp = new Timestamp(System.currentTimeMillis());
							long timestamp = logoutTimestamp.getTime();
							try {
								mClientHandler.processEvent(new BidEvent(BidEvent.BID_PLACED, timestamp, user.getName(), auctionID, amount));
							} catch (RemoteException e) {
								logger.error("Failed to connect to the Analytics Server");
							} catch (WrongEventTypeException e) {
								logger.error("Wrong type of event");
							}
						}
						logger.info(user.getName() + " bid  " + printHighestAmount(amount) + " on " + auction.getDescribtion() + " " + auction.getId());
						return answer = "You successfully bid with " + printHighestAmount(amount) + 
								" on '" + auction.getDescribtion() + "'.";
					}
					else {
						return answer = "You unsuccessfully bid with " + printHighestAmount(amount) + " on '" + 
								auction.getDescribtion() + "'. Current highest bid is " + auction.getHightestAmount() + ".";
					}
				}
			}
			answer = "The Aucion- ID '" + auctionID + "' is unknown!";
		}
		
		return answer;
	}
	
	/**
	 * method to print out an double to 2 decimals for amount
	 * 
	 * @param amount
	 * @return string to 2 decimals from a double
	 */
	public String printHighestAmount(double amount) {
		int testInt = new Double(amount*100).intValue();
		
		String inserter = "";
		if ((testInt % 10) == 0) {
			inserter += "0";
		}
		String amountString = Double.toString(amount) + inserter;
		return amountString;
	}
	
	/**
	 * @return a List of All users in the following format:
	 * ip:port - Name
	 */
	public String getAllUsers() {
		String users = "";
		
		Iterator<User> iterator = syncUserList.iterator();
		User user = null;
		while (iterator.hasNext()) {
			user = iterator.next();
			if (user.isOnline()) {
				users += user.getInternetAdress().toString().substring(1) + ":" + user.getPort() + " - " + user.getName() + "\n";
			}
		}
		return users;
	}
	
	public Timer getTimer() {
		return timer;
	}

	public synchronized void setmClientHandler(MClientHandler_RO mClientHandler) {
		this.mClientHandler = mClientHandler;
	}
	
	public int getAmountUsers() {
		Iterator<User> iterator = syncUserList.iterator();
		User user = null;
		int amount = 0;
		while (iterator.hasNext()) {
			user = iterator.next();
			if (user.isOnline()) {
				amount++;
			}
		}
		return amount;
	}
	
}
