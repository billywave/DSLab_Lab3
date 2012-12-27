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
	
	@Override
	public void run() {
		
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String userInput;

		try {
			while (((userInput = stdIn.readLine()) != null) && exit == false) {
				
				// shut dwon if input equals "!exit".
				if (userInput.equals("!exit")) {
					exit = true;
					stdIn.close();
					
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
			}
		} catch (IOException e) {
			System.out.println("Error: Failed to read from stdIn!");
		}
	}

}
