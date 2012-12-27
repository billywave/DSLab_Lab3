package loadTestingComponent;

import client.Client;

public class LoadTestListUpdater implements Runnable {
	private Client client;
	private int updateIntervalSec;
	private boolean exit = false;
	
	public LoadTestListUpdater(Client client, int updateIntervalSec) {
		this.client = client;
		this.updateIntervalSec = updateIntervalSec;
	}
	
	@Override
	public void run() {
		while(!exit) {
			client.processInput("!list");
			try {
				Thread.sleep(1000*updateIntervalSec);
			} catch (InterruptedException e) {}
			
		}
	}
	
	public void cancel() {
		exit = true;
	}

}
