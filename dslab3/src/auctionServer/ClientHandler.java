package auctionServer;

import event.UserEvent;
import exceptions.WrongEventTypeException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Timestamp;
import org.apache.log4j.Logger;
import rmi_Interfaces.MClientHandler_RO;
import security.*;

/**
 * Has the TCP connection to a cirtain client. for the requests comming over the
 * socket it uses the CommunicationProtocoll to interprate the string and react.
 *
 * @author Alexander Tatowsky
 *
 */
public class ClientHandler implements Runnable {

	private static Logger logger = Logger.getLogger(ClientHandler.class);
	Socket socket;
	// interpretation for the client- request- string
	CommunicationProtocol protocol;
	// Streams for TCP- communication
	//PrintWriter out;
	//BufferedReader in;
	Channel clientChannel;
	UserManagement userManagement;
	// for RMI
	Registry registry;
	String registryHost;
	int registryPort;
	String analyticsServerRef;
	String billingServerRef;
	MClientHandler_RO mClientHandler = null;

	public ClientHandler(Socket clientSocket, UserManagement userManagement, String analyticsServerRef, String billingServerRef) {
		this.socket = clientSocket;
		this.userManagement = userManagement;
		this.analyticsServerRef = analyticsServerRef;
		this.billingServerRef = billingServerRef;

		readProperties();
		try {
			registry = LocateRegistry.getRegistry(registryHost, registryPort);
		} catch (RemoteException e1) {
			System.out.println("Couldn't find Registry!");
		}
		try {
			mClientHandler = (MClientHandler_RO) registry.lookup(analyticsServerRef);
			userManagement.setmClientHandler(mClientHandler);
		} catch (AccessException e1) {
			logger.error("Access to the registry denied");
		} catch (RemoteException e1) {
			logger.error("Failed to connect to the Analytics Server");
		} catch (NotBoundException e1) {
			logger.error("Analytics Server not bound to the registry");
		}
	}

	@Override
	public void run() {
		
		clientChannel = new SecureServerChannel(socket);
		
//		try {
//			out = new PrintWriter(socket.getOutputStream());
//			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//		} catch (IOException e) {
//			logger.error("Failed to bind input or output stream to user client");
//		}

		// communicate
		String inputLine, outputLine;
		InetAddress inetAddress = socket.getInetAddress();
		int port = socket.getPort();
		protocol = new CommunicationProtocol(inetAddress, port, userManagement, analyticsServerRef, billingServerRef, registry);

		try {
			// read line and pass it to the CommunicationProtocoll
			while ((inputLine = clientChannel.readLine()) != null) {
				outputLine = protocol.processInput(inputLine);
				logger.debug("CH: Receiving client command: "+inputLine);
				logger.debug("CH: Sending client response: "+outputLine);
				clientChannel.println(outputLine);
				clientChannel.flush();
			}
		} catch (NullPointerException e) {
			
		} catch (IOException e) {
			// empty
		}
		if (!protocol.getUser().getName().equals("")) {
			userManagement.getUserByName(protocol.getUser().getName()).setOnline(false);
		}

		try {
			Timestamp logoutTimestamp = new Timestamp(System.currentTimeMillis());
			long timestamp = logoutTimestamp.getTime();
			mClientHandler.processEvent(new UserEvent(UserEvent.USER_DISCONNECTED, timestamp, protocol.getUser().getName()));
		} catch (RemoteException e) {
			logger.error("Failed to connect to the Analytics Server");
		} catch( NullPointerException e) {
			logger.error("Failed to connect to the Analytics Server");
		} catch (WrongEventTypeException e) {
			// wont happen
		}
	}

	/**
	 * get registry host and port from .properties
	 */
	private void readProperties() {

		java.io.InputStream is = ClassLoader.getSystemResourceAsStream("registry.properties");
		if (is != null) {
			java.util.Properties props = new java.util.Properties();
			try {
				props.load(is);
				registryHost = props.getProperty("registry.host");
				registryPort = Integer.parseInt(props.getProperty("registry.port"));
			} catch (IOException e) {
				logger.error("couldn't get IO- stream to read registry.properties");
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					logger.error("couldn't get IO- stream to read registry.properties");
				}
			}
		} else {
			logger.error("[Client1] Properties file not found!");
		}

	}

	public void shutdown() {
		if (protocol.isOnline()) {
			clientChannel.println("shutdownServer");
			clientChannel.flush();
		} else {
			clientChannel.println("Sorry, the Server just went offline. "
					+ "For re- establishing the conneciton please contact the Server- crew and restart the Client!");
		}
		//out.close();
		try {
			clientChannel.close();
			socket.close();
			userManagement.getTimer().cancel();
		} catch (IOException e) {
			System.out.println("Error: shutting down the ClientHandler failed!");
		} catch (NullPointerException e) {
			// timer was not instancated
		}
		protocol.shutdown();
	}
	
	/**
	 * close just the socket and the clientChannel to test server outage
	 */
	public void closeChannel() {
		try {
			clientChannel.close();
			socket.close();
		} catch (IOException e) {
		System.out.println("Error: shutting down the ClientHandler failed!");
		} catch (NullPointerException e) {
			// timer was not instancated
		}
	}
}
