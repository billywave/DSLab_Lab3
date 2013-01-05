package client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;

public class Client_ServerSocket implements Runnable {

	private static Logger logger = Logger.getLogger(Client_ServerSocket.class);
	
	private int tcpPort;
	private ServerSocket myServerSocket;
	private boolean listening = true;
	
	public Client_ServerSocket(int tcpPort) {
		logger.debug("setting up Clients ServerSocket on port " + tcpPort);
		this.tcpPort = tcpPort;
	}
	
	@Override
	public void run() {
		try {
			myServerSocket = new ServerSocket(tcpPort);
		} catch (IOException e) {
			logger.error("Connection to Port " + tcpPort + " can not be established");
		}
		while (listening) {
			try {
				logger.debug("Clients server socket is listening for incommings");
				Socket clientSocket = myServerSocket.accept();
				TimestampHandler timestampHandler = new TimestampHandler(clientSocket);
				Main_Client.clientExecutionService.execute(timestampHandler);
				logger.debug("Client Handler thread started");
			} catch (IOException e) {
				// empty on purpose
			}
		}
	}

}
