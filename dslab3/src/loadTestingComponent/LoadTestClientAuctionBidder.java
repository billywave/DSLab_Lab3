package loadTestingComponent;

import java.util.ArrayList;
import java.util.List;

import client.Client;
import org.apache.log4j.Logger;

public class LoadTestClientAuctionBidder implements Runnable {
	private static Logger logger = Logger.getLogger(LoadTestClientAuctionBidder.class);
	
	private Client client;
	private int bidsPerMin;
	private boolean exit = false;
	
	public LoadTestClientAuctionBidder(Client client, int bidsPerMin) {
		this.client = client;
		this.bidsPerMin = bidsPerMin;
	}

	@Override
	public void run() {
		
		// creating diversity
		try {
				Thread.sleep(LoadTestClient.rand.nextInt(60000/bidsPerMin));
		} catch (InterruptedException e) {}
		
		while(!exit) {
			List<Integer> list = new ArrayList<Integer>((LoadTester.auctions.keySet()));
			if (list.size() >= 1) {
				int randomNumber = LoadTestClient.rand.nextInt(list.size());
				int auctionId = list.get(randomNumber);
				long now = System.currentTimeMillis();
				long startTime = 0l;
				synchronized (this) {
					if (LoadTester.auctions.get(auctionId) != null) {
						startTime = LoadTester.auctions.get(auctionId);
					}
				}
				double price = ((double)(now-startTime)/1000.00);
				logger.debug("Bidding on auction " + auctionId + " for " + price);
				client.processInput("!bid " + auctionId + " " + price);
			}
			
			try {
				Thread.sleep(60000/bidsPerMin);
			} catch (InterruptedException e) {}
		}
	}
	
	public void cancel() {
		exit = true;
	}

}
