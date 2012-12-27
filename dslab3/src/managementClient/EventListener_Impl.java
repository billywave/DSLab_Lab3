package managementClient;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import rmi_Interfaces.EventListener_RO;
import event.AuctionEvent;
import event.BidEvent;
import event.Event;
import event.StatisticsEvent;
import event.UserEvent;

public class EventListener_Impl extends UnicastRemoteObject implements EventListener_RO {

	/**
	 * because of UnicastRemoteObject
	 */
	private static final long serialVersionUID = 2330328629148036611L;
	
	/*
	 * 0... automatic printing of events (!auto)
	 * 1... buffering events (!hide)
	 */
	private static byte state = 0;
	private static List<String> buffer = new ArrayList<String>();
	
	public EventListener_Impl() throws RemoteException {
		super();
	}
	
	/**
	 * print or buffer notifications from Events and Statistical Events
	 */
	@Override
	public void processEvent(Event event) throws RemoteException {
		
		Timestamp myTimestamp = new Timestamp(event.getTimestamp());
		String timestamp = new SimpleDateFormat("dd.MM.yyyy - hh:mm:ss z").format(myTimestamp);
		
		// USER_.*
		if (event instanceof UserEvent) {
			UserEvent userEvent = (UserEvent) event;
			
			// login- event
			if (userEvent.getType().equals(UserEvent.USER_LOGIN)) {
				if (state == 0) {
					System.out.println(userEvent.getType() + ": " + timestamp + " " + 
							"user " + userEvent.getUserName() + " logged in");
				} else {
					buffer.add(userEvent.getType() + ": " + timestamp + " " + 
							"user " + userEvent.getUserName() + " logged in");
				}
				
			// logout- event
			} else if (userEvent.getType().equals(UserEvent.USER_LOGOUT)) {
				if (state == 0) {
					System.out.println(userEvent.getType() + ": " + timestamp + " " + 
							"user " + userEvent.getUserName() + " logged out");
				} else {
					buffer.add(userEvent.getType() + ": " + timestamp + " " + 
							"user " + userEvent.getUserName() + " logged out");
				}
				
			// disconnection- event
			} else if (userEvent.getType().equals(UserEvent.USER_DISCONNECTED)) {
				if (state == 0) {
					System.out.println(userEvent.getType() + ": " + timestamp + " " + 
							"user " + userEvent.getUserName() + " diskonnected");
				} else {
					buffer.add(userEvent.getType() + ": " + timestamp + " " + 
							"user " + userEvent.getUserName() + " diskonnected");
				}
			}
		}
		
		// BID_*
		if (event instanceof BidEvent) {
			BidEvent bidEvent = (BidEvent) event;
			
			// bid- placed
			if (bidEvent.getType().equals(BidEvent.BID_PLACED)) {
				if (state == 0) {
					System.out.println(bidEvent.getType() + ": " + timestamp + " " + "user " + 
							bidEvent.getUserName() + " placed bid " + bidEvent.getPrice() + " on auction " + bidEvent.getAuctionID());
				} else {
					buffer.add(bidEvent.getType() + ": " + timestamp + " " + "user " + 
							bidEvent.getUserName() + " placed bid " + bidEvent.getPrice() + " on auction " + bidEvent.getAuctionID());
				}
				
			// overbid
			} else if (bidEvent.getType().equals(BidEvent.BID_OVERBID)) {
				if (state == 0) {
					System.out.println(bidEvent.getType() + ": " + timestamp + " " + "user " + 
							bidEvent.getUserName() + " overbid the auction " + bidEvent.getAuctionID() + " with " + bidEvent.getPrice());
				} else {
					buffer.add(bidEvent.getType() + ": " + timestamp + " " + "user " + 
							bidEvent.getUserName() + " overbid the auction " + bidEvent.getAuctionID() + " with " + bidEvent.getPrice());
				}
				
			// bid won
			} else if (bidEvent.getType().equals(BidEvent.BID_WON)) {
				if (state == 0) {
					System.out.println(bidEvent.getType() + ": " + timestamp + " " + "user " + 
							bidEvent.getUserName() + " won the auction " + bidEvent.getAuctionID() + " with " + bidEvent.getPrice());
				} else {
					buffer.add(bidEvent.getType() + ": " + timestamp + " " + "user " + 
							bidEvent.getUserName() + " won the auction " + bidEvent.getAuctionID() + " with " + bidEvent.getPrice());
				}
			}
		}
		
		// AUCTION_*
		if (event instanceof AuctionEvent) {
			AuctionEvent auctionEvent = (AuctionEvent) event;
			
			// auciton started
			if (auctionEvent.getType().equals(AuctionEvent.AUCTION_STARTED)) {
				if (state == 0) {
					System.out.println(auctionEvent.getType() + ": " + timestamp + " auction " + auctionEvent.getAuctionID() + " started");
				} else {
					buffer.add(auctionEvent.getType() + ": " + timestamp + " auction " + auctionEvent.getAuctionID() + " started");
				}
				
			// auction ended
			} else if (auctionEvent.getType().equals(AuctionEvent.AUCTION_ENDED)) {
				if (state == 0) {
					System.out.println(auctionEvent.getType() + ": " + timestamp + " auction " + auctionEvent.getAuctionID() + " ended");
				} else {
					buffer.add(auctionEvent.getType() + ": " + timestamp + " auction " + auctionEvent.getAuctionID() + " ended");
				}
			}
		}
		
		// STATISTICS EVENTS
		if (event instanceof StatisticsEvent) {
			StatisticsEvent statisticsEvent = (StatisticsEvent) event;
			
			// AUCTION_SUCCESS_RATIO
			if (statisticsEvent.getType().equals(StatisticsEvent.AUCTION_SUCCESS_RATIO)) {
				if (state == 0) {
					System.out.println(statisticsEvent.getType() + ": " + timestamp + " auction success ratio is " + 
							statisticsEvent.getValue() + "%");
				} else {
					buffer.add(statisticsEvent.getType() + ": " + timestamp + " auction success ratio is " + 
							statisticsEvent.getValue() + "%");
				}
				
			// AUCTION_TIME_AVG
			} else if (statisticsEvent.getType().equals(StatisticsEvent.AUCTION_TIME_AVG)) {
				if (state == 0) {
					System.out.println(statisticsEvent.getType() + ": " + timestamp + " average auction time is " + 
							(Math.round(statisticsEvent.getValue()*100)/100) + " seconds");
				} else {
					buffer.add(statisticsEvent.getType() + ": " + timestamp + " average auction time is " + 
							statisticsEvent.getValue() + " seconds");
				}
				
			// BID_COUNT_PER_MINUTE
			} else if (statisticsEvent.getType().equals(StatisticsEvent.BID_COUNT_PER_MINUTE)) {
				if (state == 0) {
					System.out.println(statisticsEvent.getType() + ": " + timestamp + " current bids per minute is " + 
							Math.round(statisticsEvent.getValue()*100)/100);
				} else {
					buffer.add(statisticsEvent.getType() + ": " + timestamp + " current bids per minute is " + 
							Math.round(statisticsEvent.getValue()*100)/100);
				}
				
			// BID_PRICE_MAX
			} else if (statisticsEvent.getType().equals(StatisticsEvent.BID_PRICE_MAX)) {
				if (state == 0) {
					System.out.println(statisticsEvent.getType() + ": " + timestamp + " maximum bid price seen so far is " +
							statisticsEvent.getValue());
				} else {
					buffer.add(statisticsEvent.getType() + ": " + timestamp + " maximum bid price seen so far is " +
							statisticsEvent.getValue());
				}
				
			// USER_SESSIONTIME_AVG
			} else if (statisticsEvent.getType().equals(StatisticsEvent.USER_SESSIONTIME_AVG)) {
				if (state == 0) {
					System.out.println(statisticsEvent.getType() + ": " + timestamp + " average session time is " + 
							(Math.round(statisticsEvent.getValue()/10)/100) + " seconds");
				} else {
					buffer.add(statisticsEvent.getType() + ": " + timestamp + " average session time is " + 
							(Math.round(statisticsEvent.getValue()/10)/100) + " seconds");
				}
				
			// USER_SESSIONTIME_MAX
			} else if (statisticsEvent.getType().equals(StatisticsEvent.USER_SESSIONTIME_MAX)) {
				if (state == 0) {
					System.out.println(statisticsEvent.getType() + ": " + timestamp + " maximum session time is " + 
							(Math.round(statisticsEvent.getValue()/10)/100) + " seconds");
				} else {
					buffer.add(statisticsEvent.getType() + ": " + timestamp + " maximum session time is " + 
							(Math.round(statisticsEvent.getValue()/10)/100) + " seconds");
				}
				
			// USER_SESSIONTIME_MIN
			} else if (statisticsEvent.getType().equals(StatisticsEvent.USER_SESSIONTIME_MIN)) {
				if (state == 0) {
					System.out.println(statisticsEvent.getType() + ": " + timestamp + " minimum session time is " + 
							(Math.round(statisticsEvent.getValue()/10)/100) + " seconds");
				} else {
					buffer.add(statisticsEvent.getType() + ": " + timestamp + " minimum session time is " + 
							(Math.round(statisticsEvent.getValue()/10)/100) + " seconds");
				}
			}
		}
	}

	protected static void printBuffer() {
		if (buffer.size() < 1) {
			System.out.println("no new events");
		}
		for (int i = 0; i < buffer.size(); i++) {
			System.out.println(buffer.get(i));
		}
		buffer.clear();
	}
	
	protected int getState() {
		return state;
	}
	
	/**
	 * 0... automatic printing of events (!auto)
	 * 1... buffering events (!hide)
	 * 
	 * @param state
	 */
	protected static synchronized void setState(byte state) {
		EventListener_Impl.state = state;
	}

	public int createID() {
		Random randomGenerator = new Random();
		return randomGenerator.nextInt();
	}
	
}
