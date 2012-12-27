package loadTestingComponent;

import org.apache.log4j.Logger;

import client.Client;
import java.util.*;

public class LoadTestClient implements Runnable {
	private Logger logger = Logger.getLogger(LoadTestClient.class);
	
	static int counter  = 0;
	private Client client;
	protected LoadTestClientAuctionCreator creator;
	protected LoadTestClientAuctionBidder bidder;
	protected LoadTestListUpdater updater;
	private static List<Integer> activeAuctions;
	
	protected static java.util.Random rand = new java.util.Random(System.currentTimeMillis());
		
	
	public LoadTestClient(String host, int tcpPort) {
		activeAuctions = Collections.synchronizedList(new ArrayList<Integer>());
		client = new Client(host, tcpPort, this);
		client.startClient();
		
		logger.debug("Logging in user"+(counter+1));
		client.processInput("!login user" + ++counter);
	}
	
	public void createAuctions(int auctionsPerMin, int auctionDuration) {
		creator = new LoadTestClientAuctionCreator(client, auctionsPerMin, auctionDuration);
		LoadTester.executorService.execute(creator);
	}
	
	public void bidAuctions(int bidsPerMin) {
		bidder = new LoadTestClientAuctionBidder(client, bidsPerMin);
		LoadTester.executorService.execute(bidder);
	}
	
	public void updateList(int updateIntervalSec) {
		updater = new LoadTestListUpdater(client, updateIntervalSec);
		LoadTester.executorService.execute(updater);
	}
	
	@Override
	public void run() {
		client.listenToAuctionServer();
	}
	
	/**
	 * Add a new time to the list of auctions
	 * @param id
	 * @param starttime 
	 */
	public void addAuctionTime(int id, long starttime) {
		LoadTester.auctions.put(id, starttime);
		activeAuctions.add(id);
		logger.debug("Adding auction" + id + ":" + starttime + " to auctions map (" + LoadTester.auctions.size() + " items)");
//		logger.debug("stored auctions: \n" + LoadTester.auctions);
//		System.out.println("\n\n NEW AUCTIONS IN LIST: " + LoadTester.auctions.size() + "\n\n");
	}
	
	/**
	 * Called by the client, when list update was received from server
	 * @param remoteListLine line of the auctions list
	 */
	public void remoteUpdate(String remoteListLine) {
		String[] splitline = remoteListLine.split(" ");
		if (splitline.length >= 4 && splitline[2].equals("Auction")) {

			/*
				* The client doesn't process the whole auctions list, but each line
				* individually and this method is called for each line.
				* Therefore I need to know, when the list is completed.
				* One way to know, is when I receive the 1. item of the next list
				* that the previous list is complete.
				* So when I come to an entry nr 1, i process the list and clear it
				* 
				* The client cannot send the whole list at once, because it
				* can only read from the stream and there would still be the
				* problem for the client to know, when the list is completed
				*/
			if (splitline[0].equals("1.")) {
				HashMap<Integer, Long> auctionsCopy = new HashMap<Integer, Long>(LoadTester.auctions);
				logger.debug("Active auctions list updated:\n" + activeAuctions);
				for (Integer auction : auctionsCopy.keySet()) {
					if (!activeAuctions.contains(auction)) {
						LoadTester.auctions.remove(auction);
					}
				}
				activeAuctions.clear();
			}
			activeAuctions.add(Integer.parseInt(splitline[4]));
		}
	}
	
	public void shutdown() {
		if (creator != null) creator.cancel();
		if (bidder != null) bidder.cancel();
		if (updater != null) updater.cancel();
		client.shutdown();
	}
}
