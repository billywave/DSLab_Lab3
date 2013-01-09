package auctionServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

public class AuctionServer_ServerSocket implements Runnable {
	private static Logger logger = Logger.getLogger(AuctionServer_ServerSocket.class);

	private int tcpPort = 0;
	private static ServerSocket myServerSocket;
	private static boolean listening = true;
	
	// save ClientHandlers in this list for expelling them when shutdown.
	static List<ClientHandler> clientHandlers = new ArrayList<ClientHandler>();
	
	UserManagement userManagement;
	
	// for RMI
	String analyticsServerRef;
	String billingServerRef;
		
	public AuctionServer_ServerSocket(int tcpPort, UserManagement userManagement, 
		String analyticsServerRef, String billingServerRef) {
		
		this.tcpPort = tcpPort;
		this.userManagement = userManagement;
		this.analyticsServerRef = analyticsServerRef;
		this.billingServerRef = billingServerRef;
	}

	/**
	 * listen to the TCP port- if a client request comes: create a new ClientHandler which
	 * establishes a connection on an other port.
	 */
	@Override
	public void run() {
		try {
			myServerSocket = new ServerSocket(tcpPort);
		} catch (IOException e) {
			logger.error("Connection to Port " + tcpPort + " can not be established");
		}
		
		logger.info("Server TCP Socket listening");
		while (AuctionServer_ServerSocket.listening) {
			try {
				Socket clientSocket = myServerSocket.accept();
				ClientHandler clientHandler = new ClientHandler(clientSocket, userManagement, analyticsServerRef, billingServerRef);
				clientHandlers.add(clientHandler);
				Main_AuctionServer.auctionServerExecutionService.execute(clientHandler);
				logger.debug("Client Handler thread started");
			} catch (IOException e) {
				// empty on purpose
			}
		}
	}

	public static void shutdown() {
		AuctionServer_ServerSocket.listening = false;
		Iterator<ClientHandler> listIterator = clientHandlers.iterator();
		while (listIterator.hasNext()) {
			listIterator.next().shutdown();
		}
		logger.debug("Server Socket Client Handler shut down");
		UserManagement.timer.cancel();
		
		try {
			myServerSocket.close();
		} catch (IOException e) {
			// socket was not open
		}
	}
	
	/**
	 * close the Socket with letting everything else alive
	 * to test the lab3- step4 (outage)
	 */
	public void closeSocket() {
		AuctionServer_ServerSocket.listening = false;
		logger.info("closing the (TCP-) ServerSocket but the Acution Server is alive");
//		UserManagement.timer.cancel();
		
		Iterator<ClientHandler> listIterator = clientHandlers.iterator();
		while (listIterator.hasNext()) {
			listIterator.next().closeChannel();
		}
		
		try {
			myServerSocket.close();
		} catch (IOException e) {
			logger.error("couldn't close the ServerSocket");
		}
	}
	
	/**
	 * open the serverSocket to test the lab3- stage4 (outage)
	 */
	public void openSocket() {
		logger.info("opening the ServerSocket");
		AuctionServer_ServerSocket.listening = true;
	}
}
