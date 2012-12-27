package analyticsServer;

import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import rmi_Interfaces.EventListener_RO;
import rmi_Interfaces.MClientHandler_RO;
import event.AuctionEvent;
import event.BidEvent;
import event.Event;
import event.StatisticsEvent;
import event.UserEvent;
import exceptions.WrongEventTypeException;

public class MClientHandler_Impl implements MClientHandler_RO {
	Logger logger = Logger.getLogger(this.getClass());

	Pattern pattern;

	List<Subscribtion> syncSubscribitionList = Collections
			.synchronizedList(new ArrayList<Subscribtion>());
	List<UserEvent> userList = Collections
			.synchronizedList(new ArrayList<UserEvent>());
	List<AuctionEvent> auctionList = Collections
			.synchronizedList(new ArrayList<AuctionEvent>());

	// USER- statistics
	StatisticsEvent minSessionTimeEvent = null;
	StatisticsEvent maxSessionTimeEvent = null;
	StatisticsEvent avgSessionTimeEvent = null;

	// AUCTION- statistics
	StatisticsEvent avgAuctionDurationTimeEvent = null;
	StatisticsEvent auctionSucessRatioEvent = null;

	// BID- statistics
	StatisticsEvent maxBidEvent = null;
	StatisticsEvent bidPerMinuteEvent = null;

	int bidCounter = 0;
	int auctionCounter = 0;
	int auctionsSuceded = 0;
	int aucitonDuration_multiplicator = 0;
	int sessiontime_avg_multiplicator = 0;

	Timestamp systemStartTimeStamp = new Timestamp(System.currentTimeMillis());
	long systemStart = systemStartTimeStamp.getTime();

	public MClientHandler_Impl() {
		// PropertyConfigurator.configure("src/log4j.properties");
	}

	@Override
	public String suscribe(EventListener_RO eventListener, String eventRegEx)
			throws RemoteException {

		// create pattern to roule out regex- format errors

		try {
			pattern = Pattern.compile(eventRegEx);
		} catch (PatternSyntaxException e) {
			return "Error: The Filter has to be a Regular Expression!";
		}

		synchronized (syncSubscribitionList) {
			Subscribtion suscribtion = new Subscribtion(pattern, eventListener);
			syncSubscribitionList.add(suscribtion);
		
		return "Created subscription with ID " + suscribtion.getID()
				+ " for events using filter " + eventRegEx;
		}
	}

	@Override
	public String unsuscribe(EventListener_RO eventListener, String suscribtionID) throws RemoteException {
		// search suscribtion in List
		synchronized (syncSubscribitionList) {
			for (int i = 0; i < syncSubscribitionList.size(); i++) {
				if (syncSubscribitionList.get(i).getID().equals(suscribtionID)) { // found
					if (eventListener.equals(syncSubscribitionList.get(i).getEventListener())) {
						syncSubscribitionList.remove(i);
						return "subscription " + suscribtionID + " terminated";
					} else return "Error: You only can unsubscribe your own subscribtion!";
				}
			}
		}
		// suscribtion can not be found
		return "Error: The Suscribtion- ID can not be found!";
	}

	@Override
	public void processEvent(Event event) throws RemoteException {
		if (event instanceof UserEvent) {
			processUserEvent((UserEvent) event);
		}
		if (event instanceof AuctionEvent) {
			processAuctionEvent((AuctionEvent) event);
		}
		if (event instanceof BidEvent) {
			processBidEvent((BidEvent) event);
		}

		notifyManagementClients(event);
	}

	/**
	 * notify all subscribtions that the Event happened. -> match regex
	 * subscribtions, ignore duplicates and delete offline mgmtclients
	 * 
	 * @param event
	 */
	private void notifyManagementClients(Event event) {

		// delete list for Clients who went offline
		List<EventListener_RO> deleteList = new ArrayList<EventListener_RO>();

		// suscribed listeners for this event
		List<EventListener_RO> notificationListForThisEvent = new ArrayList<EventListener_RO>();

		// iterator over all subscribtions
		Iterator<Subscribtion> iterator = syncSubscribitionList.iterator();
		synchronized (syncSubscribitionList) {
			while (iterator.hasNext()) {
				Subscribtion subscribtion = iterator.next();

				pattern = subscribtion.getFilterRegEx();
				Matcher matcher = pattern.matcher(event.getType());

				EventListener_RO eventListener = subscribtion
						.getEventListener();

				if (matcher.find()) {
					// make sure that multible suscribtions result in one event
					if (!notificationListForThisEvent.isEmpty()) {
						if (!notificationListForThisEvent
								.contains(eventListener)) {
//							logger.debug("EventListener will notify");
							notificationListForThisEvent.add(eventListener);
						} // else ignore -> notification already in list

						// zero eventListeners in the notification List
					} else {
//						logger.debug("EventListener will notify");
						notificationListForThisEvent.add(subscribtion
								.getEventListener());
					}
				}
			}
		}
	
		// notify all eventListeners, offline throw RemoteException ->
		// deleteList
		Iterator<EventListener_RO> notifyer = notificationListForThisEvent
				.iterator();
		while (notifyer.hasNext()) {
			EventListener_RO eventListener = null;
			try {
				eventListener = notifyer.next();
				eventListener.processEvent(event);
			} catch (RemoteException e) {
				deleteList.add(eventListener);
			}
		}
		
		synchronized (syncSubscribitionList) {
			ArrayList<Subscribtion> toDeleteSubsList = new ArrayList<Subscribtion>();
			// delete subscribtions from clients who went offline
			if (!deleteList.isEmpty()) {
				for (EventListener_RO deleter : deleteList) {
					for (Subscribtion toDeleteSubs : syncSubscribitionList) {
						if (toDeleteSubs.getEventListener().equals(deleter)) {
							toDeleteSubsList.add(toDeleteSubs);
							logger.info("deleting subscribtion "
									+ toDeleteSubs.getID()
									+ " because the client went offline");
						}
					}
				}
			}
			syncSubscribitionList.removeAll(toDeleteSubsList);
			toDeleteSubsList.clear();
		}
		deleteList.clear();
		notificationListForThisEvent.clear();
	}

	/**
	 * process any User related event like LOGIN, LOGOUT, DISCONNECT
	 * 
	 * @param event
	 */
	private void processUserEvent(UserEvent event) {

		// event is a LOGIN- Event
		if (event.getType().equals(UserEvent.USER_LOGIN)) {
			userList.add(event);
		}

		// event is a LOGOUT- or DISCONNECTED- Event
		if (event.getType().equals(UserEvent.USER_LOGOUT)
				|| event.getType().equals(UserEvent.USER_DISCONNECTED)) {

			// get login event and session time
			UserEvent loginEvent;
			long difference = 0;

			Timestamp currentTimestamp = new Timestamp(
					System.currentTimeMillis());
			long timestamp = currentTimestamp.getTime();

			for (int i = 0; i < userList.size(); i++) {
				if (userList.get(i).getUserName().equals(event.getUserName())) {
					loginEvent = userList.get(i);
					difference = event.getTimestamp()
							- loginEvent.getTimestamp(); // new session time
				}
			}

			// first minEvent
			if (minSessionTimeEvent == null) {
				try {
					minSessionTimeEvent = new StatisticsEvent(
							StatisticsEvent.USER_SESSIONTIME_MIN, timestamp,
							difference);
					notifyManagementClients(minSessionTimeEvent);
				} catch (WrongEventTypeException e) {
					logger.error("wrong Event Type");
				}
			} else {
				// current session time is less than the min -> new min
				if (minSessionTimeEvent.getValue() > difference) {
					try {
						minSessionTimeEvent = new StatisticsEvent(
								StatisticsEvent.USER_SESSIONTIME_MIN,
								timestamp, difference);
						notifyManagementClients(minSessionTimeEvent);
					} catch (WrongEventTypeException e) {
						logger.error("wrong Event Type");
					}
				}
			}

			// first maxEvent
			if (maxSessionTimeEvent == null) {
				try {
					maxSessionTimeEvent = new StatisticsEvent(
							StatisticsEvent.USER_SESSIONTIME_MAX, timestamp,
							difference);
					notifyManagementClients(maxSessionTimeEvent);
				} catch (WrongEventTypeException e) {
					logger.error("wrong Event Type");
				}
			} else {
				// current session time is bigger than the max -> new max
				if (maxSessionTimeEvent.getValue() > difference) {
					try {
						maxSessionTimeEvent = new StatisticsEvent(
								StatisticsEvent.USER_SESSIONTIME_MAX,
								timestamp, difference);
						notifyManagementClients(maxSessionTimeEvent);
					} catch (WrongEventTypeException e) {
						logger.error("wrong Event Type");
					}
				}
			}

			// set avg
			double oldAvgValue;
			if (avgSessionTimeEvent == null) {
				oldAvgValue = 0;
			} else {
				oldAvgValue = avgSessionTimeEvent.getValue();
			}

			double newValue = (oldAvgValue * sessiontime_avg_multiplicator + difference)
					/ (1 + sessiontime_avg_multiplicator);
			sessiontime_avg_multiplicator++;
			try {
				avgSessionTimeEvent = new StatisticsEvent(
						StatisticsEvent.USER_SESSIONTIME_AVG, timestamp,
						newValue);
				notifyManagementClients(avgSessionTimeEvent);
			} catch (WrongEventTypeException e) {
				logger.error("wrong Event Type");
			}
		}
	}

	/**
	 * process any Auction- related Event: AUCTION_STARTED, AUCTION_ENDED
	 */
	private void processAuctionEvent(AuctionEvent event) {

		if (event.getType().equals(AuctionEvent.AUCTION_STARTED)) {
			auctionList.add(event);
		}
		if (event.getType().equals(AuctionEvent.AUCTION_ENDED)) {

			// AVG- Duration
			if (avgAuctionDurationTimeEvent == null) {
				Timestamp currentTimestamp = new Timestamp(
						System.currentTimeMillis());
				long timestamp = currentTimestamp.getTime();
				try {
					avgAuctionDurationTimeEvent = new StatisticsEvent(
							StatisticsEvent.AUCTION_TIME_AVG, timestamp, 0);
				} catch (WrongEventTypeException e) {
					logger.error("Error: Wrong Eventtype accured");
				}
			}
			double avgDuration = (avgAuctionDurationTimeEvent.getValue()
					* aucitonDuration_multiplicator + event.getDuration())
					/ (++aucitonDuration_multiplicator);
			avgAuctionDurationTimeEvent.setValue(avgDuration);

			notifyManagementClients(avgAuctionDurationTimeEvent);

			// Auction- Sucess- Ratio
			auctionCounter++;
			if (!event.getWinner().equals("none")) {
				auctionsSuceded++;
			}
			if (auctionSucessRatioEvent == null) {
				Timestamp currentTimestamp = new Timestamp(
						System.currentTimeMillis());
				long timestamp = currentTimestamp.getTime();
				try {
					auctionSucessRatioEvent = new StatisticsEvent(
							StatisticsEvent.AUCTION_SUCCESS_RATIO, timestamp, 0);
				} catch (WrongEventTypeException e) {
					logger.error("Error: Wrong Eventtype accured");
				}
			}
			// in percent
			auctionSucessRatioEvent.setValue(100 / auctionCounter
					* auctionsSuceded);

			notifyManagementClients(auctionSucessRatioEvent);
		}

	}

	/**
	 * process any Bid- related Event: BID_PLACED, BID_OVERBID, BID_WON
	 */
	private void processBidEvent(BidEvent event) {

		// max global Bit
		if (event.getType().equals(BidEvent.BID_PLACED)
				|| event.getType().equals(BidEvent.BID_OVERBID)) {
			if (maxBidEvent == null) {
				Timestamp currentTimestamp = new Timestamp(
						System.currentTimeMillis());
				long timestamp = currentTimestamp.getTime();
				try {
					maxBidEvent = new StatisticsEvent(
							StatisticsEvent.BID_PRICE_MAX, timestamp,
							event.getPrice());
				} catch (WrongEventTypeException e) {
					logger.error("Error: Wrong Eventtype accured");
				}
			}
			if (event.getPrice() > maxBidEvent.getValue()) {
				maxBidEvent.setValue(event.getPrice());
			}
			notifyManagementClients(maxBidEvent);

			// bits per minute
			Timestamp nowTimeStamp = new Timestamp(System.currentTimeMillis());
			long now = nowTimeStamp.getTime();

			// determine minutes since systemstart- but it cannot be < 1
			long differenceInMin = (now - systemStart) / 1000 / 60;
			if ((differenceInMin) < 1) {
				differenceInMin = 1;
			}

			double bpm = (double) ++bidCounter / differenceInMin;

			if (bidPerMinuteEvent == null) {
				try {
					bidPerMinuteEvent = new StatisticsEvent(
							StatisticsEvent.BID_COUNT_PER_MINUTE, now, bpm);
				} catch (WrongEventTypeException e) {
					logger.error("Error: Wrong Eventtype accured");
				}
			} else {
				bidPerMinuteEvent.setValue(bpm);
			}
			notifyManagementClients(bidPerMinuteEvent);
		}

	}
}
