package auctionServer;

import java.util.HashSet;
import org.apache.log4j.Logger;

public class Groupbid {
	private Logger logger = Logger.getLogger(this.getClass());
	
	private User bidder;
	private HashSet<User> confirmers = new HashSet<User>();
	private User initialConfirmer;
	private double amount;
//	private int auctionID;
	private Auction auction;
	
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
	
	public boolean isInitialConfirmer(User user) {
		return initialConfirmer == user;
	}
	
	public Auction getAuction() {
		return this.auction;
	}
}
