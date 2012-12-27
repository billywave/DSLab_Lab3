package auctionServer;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimerTask;

import rmi_Interfaces.MClientHandler_RO;
import event.AuctionEvent;
import event.BidEvent;
import exceptions.WrongEventTypeException;

/**
 *
 * @author Alexander Tatowsky
 *
 */
public class Auction extends TimerTask {

	final int id = createId();
	static int ID_COUNTER = 0;
	int durationSec = 0;
	double hightestAmount = 0.0;
	int udpPort = 0;
	Calendar c;
	String endOfAuctionTimestamp = "";
	String describtion = "";
	User owner = null;
	User highestBidder = null;
	UserManagement userManagement;
	//RMI
	MClientHandler_RO mClientHandler = null;

	/**
	 * Constructor
	 *
	 * @param user
	 * @param duration
	 * @param describtion2
	 * @param udpPort
	 * @param userManagement
	 */
	public Auction(User user, int duration, String describtion2, int udpPort, UserManagement userManagement, MClientHandler_RO mClientHandler) {
		this.owner = user;
		this.durationSec = duration;
		this.describtion = describtion2;
		this.udpPort = udpPort;
		this.userManagement = userManagement;
		this.mClientHandler = mClientHandler;
		setTimestamp();
	}

	private synchronized void setTimestamp() {
		c = Calendar.getInstance();
		c.getTime();
		c.setTimeInMillis((c.getTimeInMillis() + durationSec * 1000));
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		endOfAuctionTimestamp = sdf.format(c.getTime());
	}

	public int getId() {
		return id;
	}

	public static synchronized int createId() {
		return ID_COUNTER++;
	}

	public int getDurationSec() {
		return durationSec;
	}

	public synchronized void setDurationSec(int durationSec) {
		this.durationSec = durationSec;
	}

	public double getHightestAmount() {
		return hightestAmount;
	}

	public synchronized void setHightestAmount(double hightestAmount) {
		this.hightestAmount = hightestAmount;
	}

	public User getHighestBidder() {
		if (highestBidder == null) {
			highestBidder = new User("none");
		}

		return highestBidder;
	}

	public synchronized void setHighestBidder(User highestBidder) {
		this.highestBidder = highestBidder;
	}

	public String getDescribtion() {
		return describtion;
	}

	public synchronized void setDescribtion(String describtion) {
		this.describtion = describtion;
	}

	public User getOwner() {
		return owner;
	}

	public synchronized void setOwner(User owner) {
		this.owner = owner;
	}

	public String getEndOfAuctionTimestamp() {
		return endOfAuctionTimestamp;
	}

	public String printHighestAmount() {
		int testInt = new Double(hightestAmount * 100).intValue();

		String inserter = "";
		if ((testInt % 10) == 0) {
			inserter += "0";
		}
		String amountString = Double.toString(hightestAmount) + inserter;
		return amountString;
	}

	/**
	 * the run method of the TimerTask is invoked after a given time- in this
	 * case it is invoked if the auction ends:
	 *
	 * send UDP- notifications to the owner and highest bidder
	 */
	@Override
	public void run() {
		String msg = "!auction-ended " + getHighestBidder().getName() + " " + printHighestAmount() + " " + getDescribtion();

		try {
			// if owner is online- send notification
			if (userManagement.getUserByName(owner.getName()).isOnline()) {
//				AuctionServer_UDPSocket.getInstamce().sendMessage(owner.getInternetAdress(), owner.getUdpPort(), msg);
			} else { // else save the notification until user gets online again
				userManagement.getUserByName(owner.getName()).storeNotification(msg);
			}

			// just if a highest bidder exist 
			if (!getHighestBidder().getName().equals("none")) {
				// if highest bidder is online send notification
				if (userManagement.getUserByName(getHighestBidder().getName()).isOnline()) {
//					AuctionServer_UDPSocket.getInstamce().sendMessage(getHighestBidder().getInternetAdress(), getHighestBidder().getUdpPort(), msg);
				} else {
					userManagement.getUserByName(getHighestBidder().getName()).storeNotification(msg);
				}

				// send RMI BID_WON Event
				Timestamp logoutTimestamp = new Timestamp(System.currentTimeMillis());
				long timestamp = logoutTimestamp.getTime();
				try {
					mClientHandler.processEvent(new BidEvent(BidEvent.BID_WON, timestamp, getHighestBidder().getName(), id, hightestAmount));
				} catch (WrongEventTypeException e) {
					// wont happen
				}

				// bill auction on the billing server
				// TODO for now this is static, but I'd rather obtain the protocoll instance, but where?
				CommunicationProtocol.billAuction(getHighestBidder().getName(), id, hightestAmount);

			}

			// send RMI AUCTION_ENDED Event
			Timestamp logoutTimestamp = new Timestamp(System.currentTimeMillis());
			long timestamp = logoutTimestamp.getTime();
			try {
				mClientHandler.processEvent(new AuctionEvent(AuctionEvent.AUCTION_ENDED, timestamp, this.id, durationSec, highestBidder.getName()));
			} catch (WrongEventTypeException e) {
				// wont happen
			}

			synchronized (userManagement.syncAuctionList) {
				userManagement.syncAuctionList.remove(this); // delete auction from list
			}
		} catch (IOException e) {
			System.out.println("Didn't send Notification.");
		}
	}
}
