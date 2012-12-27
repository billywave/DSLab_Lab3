package loadTestingComponent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import managementClient.ManagementClient;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class LoadTester {
	private Logger logger = Logger.getLogger(LoadTester.class);
	
	private int tcpPort;
	private String host;
	private String analyticServerRef;
	
	private ArrayList<LoadTestClient> testClients;
	private ManagementClient managementClient;
	public static ExecutorService executorService;
	//public static List<Integer> auctions = Collections.synchronizedList(new ArrayList<Integer>());
	public static Map<Integer, Long> auctions = Collections.synchronizedMap(new HashMap<Integer, Long>());
	
	private int clients;
	private int auctionsPerMin;
	private int auctionDuration;
	private int updateIntervalSec;
	private int bidsPerMin;
	
	public LoadTester() {
		executorService = Executors.newCachedThreadPool();
		testClients = new ArrayList<LoadTestClient>();
		
	}
	
	public void start(String args[]) {
		
		// TODO quit if wrong arguments
		try {
			this.host = args[0];
			this.tcpPort = Integer.parseInt(args[1]);
			this.analyticServerRef = args[2];
		} catch(NumberFormatException e) {
			logger.error("Seconds argument has to be an integer");
		} catch(ArrayIndexOutOfBoundsException e) {
			logger.error("Too few arguments");
		}
		
		PropertyConfigurator.configure("src/log4j.properties");
		
		readProperties();
		
		managementClient = new ManagementClient(analyticServerRef);
		managementClient.start();
		managementClient.processInput("!subscribe .*");
		
		for (int i = 0; i<clients; i++) {
			LoadTestClient client = new LoadTestClient(host, tcpPort);
			testClients.add(client);
			//client.loginClient();
			if (auctionsPerMin > 0) client.createAuctions(auctionsPerMin, auctionDuration);
			if (bidsPerMin > 0) client.bidAuctions(bidsPerMin);
			executorService.execute(client);
		}
		LoadTestClient updater = new LoadTestClient(host, tcpPort);
		testClients.add(updater);
		updater.updateList(updateIntervalSec);
		executorService.execute(updater);
		
		//Press enter to shut down the loadtest client
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		try {
			input.readLine();
			input.close();
		} catch (IOException ex) {
			logger.error("IO Exception on System Standard Input");
		}
		shutdown();
		
	}
	
	
	/**
	 * Reads the registry properties and stores the host and port values
	 */
	private void readProperties() {

		java.io.InputStream is = ClassLoader.getSystemResourceAsStream("loadtest.properties");
		if (is != null) {
			java.util.Properties props = new java.util.Properties();
			try {
				props.load(is);
				this.clients = Integer.parseInt(props.getProperty("clients"));
				this.auctionsPerMin = Integer.parseInt(props.getProperty("auctionsPerMin"));
				this.auctionDuration = Integer.parseInt(props.getProperty("auctionDuration"));
				this.updateIntervalSec = Integer.parseInt(props.getProperty("updateIntervalSec"));
				this.bidsPerMin = Integer.parseInt(props.getProperty("bidsPerMin"));
			} catch (IOException e) {
				logger.error("Failed to open loadtest properties");
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					logger.error("Failed to close loadtest properties");
				}
			}
		} else {
			logger.error("[Client1] Properties file not found!");
		}
	}
	
	public void shutdown() {
		logger.info("Shutting down loadtest client");
		for (LoadTestClient client : testClients) {
			client.shutdown();
		}
		managementClient.shutdown();
		executorService.shutdown();
	}
}


