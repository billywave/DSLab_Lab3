package auctionServer;

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;

import org.apache.log4j.Logger;

import rmi_Interfaces.MClientHandler_RO;
import event.BidEvent;
import exceptions.WrongEventTypeException;

public class UserManagement {
	private static Logger logger = Logger.getLogger(UserManagement.class);
	
	public static Timer timer = new Timer();
	
	final List<User> syncUserList = Collections.synchronizedList(new ArrayList<User>());
	final List<Auction> syncAuctionList = Collections.synchronizedList(new ArrayList<Auction>());
	// String: auctionid + " " + amount + " " + user
	//final Map<String, Groupbid> syncGroupBids = Collections.synchronizedMap(new HashMap<String, Groupbid>());
	
	private boolean shutdown = false;
	
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
		String auctions = "There are no auctions active!";
		
		synchronized (syncAuctionList) {
			Iterator<Auction> iterator = syncAuctionList.iterator();
			
			Auction auction;
			int i = 1;
			if (!iterator.hasNext()) {
				auctions = "There are no auctions active!";
			}
			while (iterator.hasNext()) {
				auction = iterator.next();
				if (auction.isActive()) {
				if (auctions.equals("There are no auctions active!")) auctions = "";
				auctions += (i + ". " + " Auction ID: " + auction.getId() + 
						"\n    " + auction.getDescribtion() + " - created by: " + auction.getOwner().getName() + 
						"\n    Current highest bid: " + auction.printHighestAmount() + " by: " + auction.getHighestBidder().getName() + 
						"\n    ends at: " + auction.getEndOfAuctionTimestamp() + 
						"\n----------------------------------------------\n\n");
				i++;
				}
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
					logger.debug("found auction in the syncAuctionList");
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
							} catch (NullPointerException e) {
								logger.error("Remote Object is null");
							}
							
							String msg = "!new-bid " + auction.getDescribtion();
							// send UDP- notofication that he has been oberbidden if he is online
							if (oldHighestBidder.isOnline()) {
								logger.info("would print udp- notification but this function is disabled");
								// would print UDP- notification...
//								try {
//									AuctionServer_UDPSocket.getInstamce().sendMessage(oldHighestBidder.getInternetAdress(), oldHighestBidder.getUdpPort(), msg);
//								} catch (IOException e) {
//									logger.error("Error: Trying to send UDP- Message to unknown user");
//								}
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
							} catch (NullPointerException e) {
								logger.error("Remote Object is null");
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
		logger.debug("UserManagement returns: " + answer);
		return answer;
	}
	
	public String signedBidForAuction(int auctionID, double amount, User user, Timestamp timestamp) {
		synchronized (syncAuctionList) {
			Iterator<Auction> iterator = syncAuctionList.iterator();
			while (iterator.hasNext()) {
				Auction auction = iterator.next();
				if (auction.isActive()) {
					logger.info("auciton is still active- place a normal bid");
					this.bidForAuction(user, auctionID, amount);
				} else {
					// amount is higher or you are the first bidder with this amount
					if (amount > auction.getHightestAmount() || timestamp.after(auction.getTimeOfLastBid())) {
						this.bidForAuction(user, auctionID, amount);
					} else { // some one else bid less after you- so you might be the winner because you were first
						if (timestamp.before(auction.getTimeOfLastBid()) && amount >= auction.getHightestAmount()) {
							auction.setHightestAmount(amount);
							auction.setHighestBidder(user);

							logger.debug("bidding for signed auciton because you were first");
							return "You successfully bid with "
									+ auction.getHightestAmount() + " on '"
									+ auction.getDescribtion()
									+ "'. Current highest bid is "
									+ auction.getHightestAmount() + ".";
						}
					}
				}
//				if (auction.getId() == auctionID
//						&& amount > auction.getHightestAmount()
//						&& timestamp.before(new Timestamp(auction.getStartedTimestamp()
//								+ auction.getSpareDuration()))
//						&& timestamp.after(new Timestamp(auction.getStartedTimestamp()))) {
//
//					auction.setHightestAmount(amount);
//					auction.setHighestBidder(user);
//
//					logger.debug("bidding for signed auciton");
//					return "You successfully bid with "
//							+ auction.getHightestAmount() + " on '"
//							+ auction.getDescribtion()
//							+ "'. Current highest bid is "
//							+ auction.getHightestAmount() + ".";
//				}
			}
		}
		return "Bidding was not successful: either your amount was not the hightest or the auction already was closed.";
	}
	
	/**
	 * Putting a groupBid on wait for confirmation
	 * @param auctionID
	 * @param amount
	 * @param user
	 * @return
	 */
	public String groupBidForAuction(int auctionID, double amount, User user) {
		//synchronized (syncAuctionList) {
		
		Iterator<Auction> iterator = syncAuctionList.iterator();
		Auction auction;
		
		while (iterator.hasNext()) {
			auction = iterator.next();
			if (auction.getId() == auctionID) {
				if (!Groupbid.groupBidAllowed(auction, user, getAmountUsers())) {
					logger.debug("groupbid denied");
					Groupbid.deniedRequest(user, timer);
					
					return "No more groupbids allowed";
				}
				
				if (amount > auction.getHightestAmount()) {
					Groupbid groupbid = new Groupbid(auction, amount, user);
					groupbid.addGroupBid();

					logger.info(user.getName() + " put a groupBid  " + printHighestAmount(amount) + " on " + auction.getDescribtion() + " " + auction.getId());
					return "You successfully put a groupBid with " + printHighestAmount(amount) + 
							" on '" + auction.getDescribtion() + "', that awaits confirmation";
				}
				else {
					return "You unsuccessfully put a  groupBid with " + printHighestAmount(amount) + " on '" + 
							auction.getDescribtion() + "'. Current highest bid is " + auction.getHightestAmount() + ".";
				}
			}
		}

		return "!rejected The Aucion- ID '" + auctionID + "' is unknown!";
	}
	
	/**
	 * Confirming an existing groupbid
	 * @param auctionID
	 * @param amount
	 * @param biddingUser
	 * @param confirmer
	 * @return 
	 */
	public String confirmGroupBid(int auctionID, double amount, User biddingUser, User confirmer) {
		String key = auctionID + " " + amount + " " + biddingUser.getName();
		

		Groupbid bid = Groupbid.getGroupBid(key);
		if (bid == null) return "!rejected GroupBid not found";

		Auction auction = bid.getAuction();

		boolean confirming = bid.confirm(confirmer, this.getAmountUsers());
		if (!confirming) return "!rejected too many blocked clients";
		int counter = 0;
		while(!shutdown && !bid.greenlid() && amount > auction.getHightestAmount()) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException ex) {}
			counter++;
			if (counter >= 60) return "!rejected out of time";
		}

		if (bid.greenlid() && amount > auction.getHightestAmount()) {
			logger.debug("Enough confirms received");
			if (bid.isInitialConfirmer(confirmer)) {
				logger.debug("Initial confirmer: " + confirmer.getName());
				synchronized (syncAuctionList) {
					bid.execute();
				}
			} else logger.debug("Second confirmer: " + confirmer.getName());
			return "!confirmed";

		} else {
			// need the followin line for the case, that A bids too soon and
			// therefore the case is: amount == auction.getHighestAmount()
			if (bid.executed()) return "!confirmed";
			return "!rejected amount not high enough";
		}

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
				users += user.getInternetAdress().toString().substring(1) + ":" + user.getUdpPort() + " - " + user.getName() + "\n";
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
	
	/**
	 * if auction server comes back in Lab3- Stage4, aucitons which should be running
	 * are reset active.
	 
	public synchronized void resetAuctions() {
		synchronized (syncAuctionList) {
				for (int i = 0; i < syncAuctionList.size(); i++) {
					Auction auction = syncAuctionList.get(i);
					
					// if there is some time left -> restart the auction
					if (new Timestamp(auction.getInterruptedTimestamp()
							+ auction.getSpareDuration()).after(new Timestamp(
							System.currentTimeMillis()))) {
						
						auction.setActive(true);
						long newDurationSec = (auction.getDurationSec() * 1000 - (System.currentTimeMillis() - 
								auction.getInterruptedTimestamp())) / 1000;
						logger.debug("set spare Duration to: " + (newDurationSec)
								+ " Seconds");

						auction.setDurationSec((int) newDurationSec);

						Auction newAuction = new Auction(auction);
						newAuction.setActive(true);

						syncAuctionList.remove(auction);
						syncAuctionList.add(newAuction);

						try {
							logger.debug("new Auction got started with "
									+ newDurationSec + " seconds duration");

							timer.schedule(newAuction,
									newAuction.durationSec * 1000);
						} catch (IllegalStateException e) {
							logger.warn("timer was cancelt - restarting timer");
							timer = new Timer();
							timer.schedule(newAuction,
									newAuction.durationSec * 1000);
						}
					}
				}
		}
	}
	*/
	
	public void shutdown() {
		this.shutdown = true;
	}
	
}
