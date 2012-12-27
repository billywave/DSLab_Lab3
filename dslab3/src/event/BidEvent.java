package event;

import exceptions.WrongEventTypeException;

public class BidEvent extends Event {

	/**
	 * because of serialisazion
	 */
	private static final long serialVersionUID = 4111505522425282065L;

	public static final String BID_PLACED = "BID_PLACED";
	public static final String BID_OVERBID = "BID_OVERBID";
	public static final String BID_WON = "BID_WON";
	
	private String userName;
	private long auctionID;
	private double price;
	
	public BidEvent(String type, long timestamp, String userName, long auctionID, double price) throws WrongEventTypeException {
		if (!(type.equals(BID_PLACED) || type.equals(BID_OVERBID) || type.equals(BID_WON))) {
			throw new WrongEventTypeException();
		}
		this.type = type;
		this.timestamp = timestamp;
		this.userName = userName;
		this.auctionID = auctionID;
		this.price = price;
	}

	public String getUserName() {
		return userName;
	}

	public synchronized void setUserName(String userName) {
		this.userName = userName;
	}

	public long getAuctionID() {
		return auctionID;
	}

	public synchronized void setAuctionID(long auctionID) {
		this.auctionID = auctionID;
	}

	public double getPrice() {
		return price;
	}

	public synchronized void setPrice(double price) {
		this.price = price;
	}
}
