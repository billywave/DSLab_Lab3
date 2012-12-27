package event;

import exceptions.WrongEventTypeException;


public class AuctionEvent extends Event {

	/**
	 * because of Serialization 
	 */
	private static final long serialVersionUID = -9192709391297498739L;
	public static final String AUCTION_STARTED = "AUCTION_STARTED";
	public static final String AUCTION_ENDED = "AUCTION_ENDED";
	
	private long auctionID;
	private int duration;
	private String winner;
	
	public AuctionEvent(String type, long timestamp, long auctionID) throws WrongEventTypeException {
		if (!(type.equals(AUCTION_STARTED) || type.equals(AUCTION_ENDED))) {
			throw new WrongEventTypeException();
		}
		this.type = type;
		this.timestamp = timestamp;
		this.auctionID = auctionID;
	}
	
	public AuctionEvent(String type, long timestamp, long auctionID, int duration, String winner) 
			throws WrongEventTypeException {
		
		if (!(type.equals(AUCTION_STARTED) || type.equals(AUCTION_ENDED))) {
			throw new WrongEventTypeException();
		}
		this.type = type;
		this.timestamp = timestamp;
		this.auctionID = auctionID;
		this.duration = duration;
		this.winner = winner;
	}

	public long getAuctionID() {
		return auctionID;
	}

	public synchronized void setAuctionID(long auctionID) {
		this.auctionID = auctionID;
	}

	public int getDuration() {
		return duration;
	}

	public synchronized void setDuration(int duration) {
		this.duration = duration;
	}

	public String getWinner() {
		return winner;
	}

	public synchronized void setWinner(String winner) {
		this.winner = winner;
	}
	
	
}
