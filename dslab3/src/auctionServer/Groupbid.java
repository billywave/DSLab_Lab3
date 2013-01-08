/*
 * TODO 
 * the number of active auctions with group bids is less than or equal 
 * to the number of group members
 * 
 * block client
 * 
 * 
 */

package auctionServer;

import java.util.*;
import org.apache.log4j.Logger;
import rmi_Interfaces.MClientHandler_RO;

public class Groupbid {
	private Logger logger = Logger.getLogger(this.getClass());
	
	// String: auctionid + " " + amount + " " + user
	private final static Map<String, Groupbid> syncGroupBids = Collections.synchronizedMap(new HashMap<String, Groupbid>());
	private final static Set<Auction> activeAuctionsWithGroupBids = Collections.synchronizedSet(new HashSet<Auction>());
	private final static Set<Groupbid> syncBlockingGroupBids = Collections.synchronizedSet(new HashSet<Groupbid>());
	
	// starvation
	private final static Map<User, Integer> groupBidDenials = Collections.synchronizedMap(new HashMap<User, Integer>());
	
	private User bidder;
	private Set<User> confirmers = Collections.synchronizedSet(new HashSet<User>());
	private User initialConfirmer;
	private double amount;
//	private int auctionID;
	private Auction auction;
	private boolean executed;
	
	public Groupbid(Auction auction, double amount, User bidder) {
		this.auction = auction;
		this.amount = amount;
		this.bidder = bidder;
	}
	
	public static boolean groupBidAllowed(Auction auction, int amountUsers) {
		int sumA = Groupbid.sumActiveAuctionsWithGroupBids();
		return sumA > amountUsers || (sumA == amountUsers && !Groupbid.hasGroupBid(auction));
	}
	
	public static void deniedRequest(User user, Timer timer) {
		try {
			timer.schedule(new GroupBidDenial(user), 20000);
		} catch (IllegalStateException e) {
			Logger.getLogger(Groupbid.class).warn("timer is already cancelt");
		}
	}
	
	public void addGroupBid() {
		syncGroupBids.put(auction.id + " " + amount + " " + bidder.getName(), this);
		activeAuctionsWithGroupBids.add(auction);
	}
	
	public static Groupbid getGroupBid(String key) {
		if (syncGroupBids.containsKey(key)) return syncGroupBids.get(key);
		return null;
	}
	
//	public void removeGroupBid(String key) {
//		if (syncGroupBids.containsKey(key)) {
//			Auction a = syncGroupBids.get(key).getAuction();
//			syncGroupBids.remove(key);
//			
//			
//			
//		} else logger.error("Groupbid not found");
//	}
	
	public boolean confirm(User user, int onlineUsers) {
		// checking for deadlock prevention
		if (syncBlockingGroupBids.size()+1 < onlineUsers || syncBlockingGroupBids.contains(this)) {
			if (initialConfirmer == null) initialConfirmer = user;
			confirmers.add(user);

			if (confirmers.size() < 2) syncBlockingGroupBids.add(this);
			else syncBlockingGroupBids.remove(this);

			logger.debug("Groupbid " + amount + " from " + bidder.getName() + " on auction id " + auction.id + " confirmed by " + user.getName());
			
			return true;
		}
		return false;
	}
	
	public boolean greenlid() {
		return confirmers.size() >= 2;
	}
	
	// returns true, if the groupbid was turned confirmend and acutally placed into the auction
	public boolean executed() {
		return executed;
	}
	
	public void execute(MClientHandler_RO mClientHandler) {
		logger.debug("Calling execute method");
		if (!executed) {


			User oldHighestBidder = auction.getHighestBidder();

			auction.setHightestAmount(amount);
			auction.setHighestBidder(bidder);
			logger.debug("Bid placed");

			// not sure if the following also applies to groupBids
			// -- BEGIN --
//			if (!oldHighestBidder.getName().equals("none")) {
//
//				//RMI- BID_PLACED
//				Timestamp logoutTimestamp = new Timestamp(System.currentTimeMillis());
//				long timestamp = logoutTimestamp.getTime();
//				try {
//					mClientHandler.processEvent(new BidEvent(BidEvent.BID_OVERBID, timestamp, bidder.getName(), auction.id, amount));
//				} catch (RemoteException e) {
//					logger.error("Failed to connect to the Analytics Server");
//				} catch (WrongEventTypeException e) {
//					logger.error("Wrong type of event");
//				}
//
//				String msg = "!new-bid " + auction.getDescribtion();
//				// send UDP- notofication that he has been oberbidden if he is online
//				if (oldHighestBidder.isOnline()) {
//					try {
//						AuctionServer_UDPSocket.getInstamce().sendMessage(oldHighestBidder.getInternetAdress(), oldHighestBidder.getUdpPort(), msg);
//					} catch (IOException e) {
//						System.out.println("Error: Trying to send UDP- Message to unknown user");
//					}
//				} else {
//					// if he is not online store the notification
//					oldHighestBidder.storeNotification(msg);
//				}
//			} else {
//				//RMI- BID_PLACED
//				Timestamp logoutTimestamp = new Timestamp(System.currentTimeMillis());
//				long timestamp = logoutTimestamp.getTime();
//				try {
//					mClientHandler.processEvent(new BidEvent(BidEvent.BID_PLACED, timestamp, bidder.getName(), auction.id, amount));
//				} catch (RemoteException e) {
//					logger.error("Failed to connect to the Analytics Server");
//				} catch (WrongEventTypeException e) {
//					logger.error("Wrong type of event");
//				}
//			}
			// -- END --
			
			syncGroupBids.remove(auction.id + " " + amount + " " + bidder.getName());
			syncBlockingGroupBids.remove(this);
			
			// look for other groupbids on same auction
			boolean found = false;
			for (Groupbid gb : syncGroupBids.values()) {
				if (gb.getAuction() == auction) found = true;
			}
			if (!found) activeAuctionsWithGroupBids.remove(auction);
			
			logger.debug("Execution finished");
			executed = true;
		}
	}
	
	public boolean isInitialConfirmer(User user) {
		return initialConfirmer == user;
	}
	
	public Auction getAuction() {
		return this.auction;
	}
	
	public static int sumGroupBids() {
		return syncGroupBids.size();
	}
	
	public static int sumActiveAuctionsWithGroupBids() {
		int n = 0;
		for (Auction a : activeAuctionsWithGroupBids) {
			if (a.isActive()) n++ ;
		}
		return n;
	}
	
	public static boolean hasGroupBid(Auction auction) {
		return activeAuctionsWithGroupBids.contains(auction);
	}
}
