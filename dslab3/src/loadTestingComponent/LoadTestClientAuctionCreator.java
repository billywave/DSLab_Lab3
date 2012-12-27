package loadTestingComponent;

import client.Client;
import org.apache.log4j.Logger;

public class LoadTestClientAuctionCreator implements Runnable {
	private static Logger logger = Logger.getLogger(LoadTestClientAuctionCreator.class);
	
	private Client client;
	private int auctionsPerMin;
	private int auctionDuration;
	private boolean exit = false;
	
	public LoadTestClientAuctionCreator(Client client, int auctionsPerMin, int auctionDuration) {
		this.client = client;
		this.auctionsPerMin = auctionsPerMin;
		this.auctionDuration = auctionDuration;
	}

	@Override
	public void run() {
		
		// creating diversity
		try {
				Thread.sleep(LoadTestClient.rand.nextInt(60000/auctionsPerMin));
		} catch (InterruptedException e) {}
		
		while(!exit) {
			logger.debug("Creating auction for " + auctionDuration + "ms");
			client.processInput("!create " + auctionDuration + " testauction"); // give LoadTester
			
			try {
				Thread.sleep(60000/auctionsPerMin);
			} catch (InterruptedException e) {}
		}
	}
	
	public void cancel() {
		exit = true;
	}

}
