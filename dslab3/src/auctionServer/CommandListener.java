package auctionServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
/**
 * listen to command line input. if the input equals "" -> shut down.
 * 
 * @author Alexander Tatowsky
 *
 */
public class CommandListener implements Runnable {

	private boolean exit = false;
	AuctionServer_ServerSocket auctionServer_ServerSocket;
	
	public CommandListener(AuctionServer_ServerSocket auctionServer_ServerSocket) {
		this.auctionServer_ServerSocket = auctionServer_ServerSocket;
	}
	
	@Override
	public void run() {
		
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String userInput;

		try {
			while (((userInput = stdIn.readLine()) != null) && exit == false) {
				
				// shut dwon if input equals "!exit".
				if (userInput.equals("!exit")) {
					exit = true;
					try {
						stdIn.close();
					} catch (Exception e) {
						System.out.println("closing the BufferedReader didn't work");
					}
					
					AuctionServer_ServerSocket.shutdown();
					
					// propper shutdown of the ExecutorService
					Main_AuctionServer.auctionServerExecutionService.shutdown(); // Disable new tasks from being submitted
					try {
						// Wait a while for existing tasks to terminate
						if (!Main_AuctionServer.auctionServerExecutionService.awaitTermination(3, TimeUnit.SECONDS)) {
							Main_AuctionServer.auctionServerExecutionService.shutdownNow(); // Cancel currently executing tasks
							// Wait a while for tasks to respond to being cancelled
							if (!Main_AuctionServer.auctionServerExecutionService.awaitTermination(3, TimeUnit.SECONDS))
								System.err.println("Pool did not terminate");
							}
					} catch (InterruptedException ie) {
						// (Re-)Cancel if current thread also interrupted
						Main_AuctionServer.auctionServerExecutionService.shutdownNow();
						// Preserve interrupt status
						Thread.currentThread().interrupt();
					}
					break;
				}	
				
				// close the TCP- ServerSocket to test the server outage function (lab3- stage4)
				if (userInput.equals("!close")) {
					auctionServer_ServerSocket.userManagement.loggoutAll();
					auctionServer_ServerSocket.closeSocket();
					AuctionServer.setServerIsOnline(false);
				}
				
				// open the TCP- ServerSocket again to test the server outage function
				if (userInput.equals("!reconnect")) {
					auctionServer_ServerSocket.openSocket();
					Main_AuctionServer.auctionServerExecutionService.execute(auctionServer_ServerSocket);
					AuctionServer.setServerIsOnline(true);
//					auctionServer_ServerSocket.userManagement.resetAuctions();
				}
			}
		} catch (IOException e) {
			System.out.println("Error: Failed to read from stdIn!");
		}
	}

}
