package auctionServer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * just create an Thread- Pool- ExecutorService and an 
 * AuctionServer- which is the non- static ServerObject.
 * 
 * @author Alexander Tatowsky
 *
 */
public class Main_AuctionServer {

	// thread pool- handler
	public static ExecutorService auctionServerExecutionService;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		auctionServerExecutionService = Executors.newCachedThreadPool();
		
		try {
			AuctionServer auctionServer = new AuctionServer(args);
			auctionServer.startServer();
		} catch (NumberFormatException e) {
			System.out.println("Something was wrong with the args[] arguments");
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Something was wrong with the args[] arguments");
		}		
	}

}
