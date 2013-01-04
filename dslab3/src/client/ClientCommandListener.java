package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import security.Channel;

public class ClientCommandListener implements Runnable {

	private Logger logger = Logger.getLogger(this.getClass());
	
	BufferedReader stdIn;
	private static boolean exit = false;
	Socket socket = null;
	//PrintWriter out = null;
	private Channel serverChannel;
	int udpPort = 0;

	Client client;

	public ClientCommandListener(Channel serverChannel, int udpPort, Client client) {
		//this.socket = socket;
		//this.udpPort = udpPort;
		this.client = client; // for shutting down
		
		this.serverChannel = serverChannel;
//		try {
//			out = new PrintWriter(socket.getOutputStream());
//		} catch (IOException e) {
//			logger.error("Error: Failed to obtain server output stream!");
//		} catch (NullPointerException e) {
//			logger.error("Error: Failed to obtain server output stream!");
//		}
	}

	@Override
	public void run() {

		stdIn = new BufferedReader(new InputStreamReader(System.in));
		String userInput;

		try {
			
			while (exit == false && ((userInput = stdIn.readLine()) != null)) {
				processInput(userInput);
			}
			// stutdown when exit
			if (exit == true) {

				stdIn.close();

				// propper shutdown of the ExecutorService
				Main_Client.clientExecutionService.shutdown(); // Disable new
																// tasks from
																// being
																// submitted
				try {
					// Wait a while for existing tasks to terminate
					if (!Main_Client.clientExecutionService.awaitTermination(3,
							TimeUnit.SECONDS)) {
						Main_Client.clientExecutionService.shutdownNow(); // Cancel
																			// currently
																			// executing
																			// tasks
						// Wait a while for tasks to respond to being cancelled
						if (!Main_Client.clientExecutionService
								.awaitTermination(3, TimeUnit.SECONDS))
							System.err.println("Pool did not terminate");
					}
				} catch (InterruptedException ie) {
					// (Re-)Cancel if current thread also interrupted
					Main_Client.clientExecutionService.shutdownNow();
					// Preserve interrupt status
					Thread.currentThread().interrupt();
				}

				client.shutdown();
			}
		} catch (IOException e) {
			System.out.println("Error: Failed to read from stdIn!");
		}
	}

	protected void processInput(String userInput) {
		if (userInput.equals("!end")) {
			exit = true;
			try {
				stdIn.close();
			} catch (IOException e) {
				logger.error("coulden't get inputstream");
			}

			// propper shutdown of the ExecutorService
			Main_Client.clientExecutionService.shutdown(); // Disable new tasks
															// from being
															// submitted
			try {
				// Wait a while for existing tasks to terminate
				if (!Main_Client.clientExecutionService.awaitTermination(3,
						TimeUnit.SECONDS)) {
					Main_Client.clientExecutionService.shutdownNow(); // Cancel
																		// currently
																		// executing
																		// tasks
					// Wait a while for tasks to respond to being cancelled
					if (!Main_Client.clientExecutionService.awaitTermination(3,
							TimeUnit.SECONDS))
						System.err.println("Pool did not terminate");
				}
			} catch (InterruptedException ie) {
				// (Re-)Cancel if current thread also interrupted
				Main_Client.clientExecutionService.shutdownNow();
				// Preserve interrupt status
				Thread.currentThread().interrupt();
			}

			client.shutdown();
			exit = true;
		} else {
			String[] commandArray = userInput.split(" ");
			if (commandArray[0].equals("!login")) {
				userInput += " " + udpPort;
				/*
				try {
					UDPSocket.username = commandArray[1];
				} catch (ArrayIndexOutOfBoundsException e) {

				}*/

			}
			logger.debug("sending TCP- message: " + userInput);
			serverChannel.println(userInput);
			serverChannel.flush();
			
			/**
			 * TODO why doesn't this get done?
			 */
			// obtain list of other users
			serverChannel.println("!getFirstClientList");
			serverChannel.flush();
		}
		
	}

	public void setServerChannel(Channel serverChannel) {
		this.serverChannel = serverChannel;
	}
	
	public static void shutdown() {
		ClientCommandListener.exit = true;
	}

}
