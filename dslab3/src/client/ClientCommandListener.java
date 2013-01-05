package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
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
	boolean serverIsOnline = true;
	
	public ClientCommandListener(Channel serverChannel, int udpPort, Client client) {
		//this.socket = socket;
		this.udpPort = udpPort;
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
			if (commandArray.length > 0 && commandArray[0].equals("!login")) {
				userInput += " " + udpPort;
				/*
				try {
					UDPSocket.username = commandArray[1];
				} catch (ArrayIndexOutOfBoundsException e) {
				}*/
				
				logger.debug("sending TCP- message: " + userInput);
				serverChannel.println(userInput);
				serverChannel.flush();

				// obtain list of other users
				serverChannel.println("!getFirstClientList");
				serverChannel.flush();
			} 
			else if (commandArray.length > 0 && commandArray[0].equals("!bid") && !serverIsOnline) {
				logger.debug("trying to get signed timestamp");
				
				// request a signed timestamp from 2 online clients
				if (client.onlineUsers.size() < 1) {
					logger.error("Bid cannot be signed- too less users are online");
				} else {
					Random randomGenerator = new Random();
					int random1 = randomGenerator.nextInt(client.onlineUsers.size());
					int random2 = -1;
					OnlineUser signer1;
					OnlineUser signer2;
					
					// pick a random second signer which is not the fist and not you self
					while (random2 < 0 && random1 != random2) {
						random2 = randomGenerator.nextInt(client.onlineUsers.size()-1);
						signer2 = client.onlineUsers.get(random2);
						if (signer2.getPort() == udpPort) {   // himself -> search goes on
							random2 = -1;
						}
					}
					signer1 = client.onlineUsers.get(random1);
					signer2 = client.onlineUsers.get(random2);
					
					if (commandArray.length <= 3) {
						reiciveSignedTimestamp(signer1, commandArray);
						reiciveSignedTimestamp(signer2, commandArray);
					}
					
				}
				
			} else {
				logger.debug("sending TCP- message: " + userInput);
				serverChannel.println(userInput);
				serverChannel.flush();
			}
		}
		
	}

	private String reiciveSignedTimestamp(OnlineUser signer, String[] commandArray) {
		PrintWriter out = null;
        BufferedReader in = null;
		String signedAnswer = "";
		
		try {
			Socket socket1 = new Socket(signer.getIP(), signer.getPort());
			out = new PrintWriter(socket1.getOutputStream(), true);
	        in = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		String auctionID = commandArray[1];
		String price = commandArray[2];
		logger.debug("request signed timestamp with msg: " + "!getTimestamp " + auctionID + " " + price);
		out.print("!getTimestamp " + auctionID + " " + price + "\n");
		out.flush();
		try {
			signedAnswer = in.readLine();
			logger.debug("got signed answer: " + signedAnswer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return signedAnswer;
	}
	
	public void setServerChannel(Channel serverChannel) {
		this.serverChannel = serverChannel;
	}
	
	public static void shutdown() {
		ClientCommandListener.exit = true;
	}

}
