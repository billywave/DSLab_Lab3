package auctionServer;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Instance of the AuctionServer. 
 * Creates an UserManagement Instance (which reference will be passed to all objects who need it)
 * and a CommandListener.
 * 
 * @author Alexander Tatowsky
 *
 */
public class AuctionServer {
	private static Logger logger = Logger.getLogger(AuctionServer.class);

	int tcpPort = 0;
	AuctionServer_ServerSocket tcpSocket;
	
	// for RMI
	String analyticsServerRef;
	String billingServerRef;
	
	public AuctionServer(String[] args) throws NumberFormatException, ArrayIndexOutOfBoundsException {
		this.tcpPort = Integer.parseInt(args[0]);
		this.analyticsServerRef = args[1];
		this.billingServerRef = args[2];
	}
	
	public void startServer() {
		PropertyConfigurator.configure("src/log4j.properties");        
		
		UserManagement userManagement = new UserManagement();
		
		tcpSocket = new AuctionServer_ServerSocket(tcpPort, userManagement, analyticsServerRef, billingServerRef);
		Main_AuctionServer.auctionServerExecutionService.execute(tcpSocket);
		logger.debug("Server Socket thread started");
		
		CommandListener cmdListener = new CommandListener(tcpSocket);
		Main_AuctionServer.auctionServerExecutionService.execute(cmdListener);
		logger.debug("CommandListener thread started");
		
	}
}
