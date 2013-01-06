package auctionServer;

import event.BidEvent;
import exceptions.WrongEventTypeException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.HashSet;
import org.apache.log4j.Logger;
import rmi_Interfaces.MClientHandler_RO;

public class Groupbid {
	private Logger logger = Logger.getLogger(this.getClass());
	
	private User bidder;
	private HashSet<User> confirmers = new HashSet<User>();
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
	
	public void confirm(User user) {
		if (initialConfirmer == null) initialConfirmer = user;
		confirmers.add(user);
		logger.debug("Groupbid " + amount + " from " + bidder.getName() + " on auction id " + auction.id + " confirmed by " + user.getName());
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
}
