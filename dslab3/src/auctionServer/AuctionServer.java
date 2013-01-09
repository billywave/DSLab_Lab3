package auctionServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import security.RSAChannel;

import security.SecureServerChannel;

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
		String password = "";
		do {
			try {
				System.out.println("Enter pass phrase for RSA Private key:");
				password = (new BufferedReader(new InputStreamReader(System.in)).readLine());
				if (password.equals("!exit")) return;
				SecureServerChannel.setServerPrivateKeyPassword(password);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		} while (RSAChannel.readPrivateKey("auction-server", password) == null);
		
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
