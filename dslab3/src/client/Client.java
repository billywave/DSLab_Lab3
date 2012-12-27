package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import loadTestingComponent.LoadTestClient;

import org.apache.log4j.Logger;

/**
 * This class represents the Client and is the first Object 
 * which will be instancated. Main functions are: 
 * - listening to a UDP- Port 
 * - listening to commands
 * - creating a TCP- Socket to the Server
 * 
 * @author Alexander Tatowsky
 *
 */
public class Client {

	private Logger logger = Logger.getLogger(Client.class);
	
	String host;
	int tcpPort;
	int udpPort;
	
	boolean ioNotFound = false;
	boolean isTestingClient = false;
	
	Socket socket = null;
    BufferedReader in = null;
    //UDPSocket udpSocket;
    
    ClientCommandListener commandListener;
    
    private LoadTestClient loadTestClient;
    
    /**
     * Constructor;
     * 
     * @param args
     * 
     * args[0]: host: host name or IP of the auction server
	 * args[1]: TCP connection port on which the auction server is listening for incoming connections
	 * args[2]: this port will be used for instantiating a java.net.DatagramSocket
	 * 
	 * @throws NumberFormatException, ArrayIndexOutOfBoundsException if the arguments cannot be read
     */
	public Client(String args[]) throws NumberFormatException, ArrayIndexOutOfBoundsException {
			this.host = args[0];
			this.tcpPort = Integer.parseInt(args[1]);
			//this.udpPort = Integer.parseInt(args[2]);	
			//logger.setLevel(Level.ERROR);
	}
	
	/**
	 * Constructor for the loadtest
	 * @param host
	 * @param tcpPort 
	 */
	public Client(String host, int tcpPort, LoadTestClient loadTestClient) {
			this.host = host;
			this.tcpPort = tcpPort;
			this.isTestingClient = true;
			this.loadTestClient = loadTestClient;
	}

	/**
	 * Starting the Client in sense of 
	 *   - listeneing on a UDP- Socket
	 *   - listening to command- line- input
	 */
	public void startClient() {
		logger.debug("starting Client");
		
        //udpSocket = new UDPSocket(udpPort);
        //Main_Client.clientExecutionService.execute(udpSocket);
        try {
            socket = new Socket(host, tcpPort);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	        } catch (UnknownHostException e) {
	            System.err.println("Don't know about host: " + host + ".");
	        } catch (IOException e) {
	            System.err.println("Couldn't get I/O for the connection to: " + host + ".\n" +
	            		"Please try again later!");
	            
	            ioNotFound = true;
	            shutdown();
	            // propper shutdown of the ExecutorService
	            Main_Client.clientExecutionService.shutdown(); // Disable new tasks from being submitted
        }
		
		commandListener = new ClientCommandListener(socket, udpPort, this);
    	
		if (!isTestingClient) {
			Main_Client.clientExecutionService.execute(commandListener);
			this.listenToAuctionServer();
		}
    }
	
	/**
	 * listen to the server
	 */
	public void listenToAuctionServer() {
		// just build if the connection to the server is ok.
        if (ioNotFound == false) {
        	String incommingMessage;
       	 
    	    try {
    	    	while((incommingMessage = in.readLine()) != null) {
    	    		// server shut down
    	    		if (incommingMessage.equals("shutdownServer")) {
    	    			System.out.println("Sorry, the Server just went offline, you have been logged out automatically. " +
					"Please press ENTER to shut down your Client and try to start it again later!");
    	    			shutdown();
    	    			ClientCommandListener.shutdown();
    	    		}
    	    		else {
    	    			// if testing- dont print out, else print out
    	    			if (this.isTestingClient && loadTestClient != null) {
    	    				// send list answer to updaterClient in Testing Component
    	    				loadTestClient.remoteUpdate(incommingMessage);
        	    			// Testing Component: add new auction to list
        	    			String splitLine[] = incommingMessage.split(" ");
        	    			if (splitLine.length >= 6 && splitLine[4].equals("id")) {
        	    				loadTestClient.addAuctionTime(Integer.parseInt(splitLine[5]), System.currentTimeMillis());
        	    			}
    	    			} else {
    	    				System.out.println(incommingMessage);
    	    			}
    	    			
    	    		}
    			}
    		} catch (IOException e) {
    			// empty- just is thrown when ending client
    		}
        }
	}
	
	/**
	 * Processing input from the loadtest client
	 * @param userInput 
	 */
	public void processInput(String userInput) {
		logger.debug("calling processInput(" + userInput + ") in ClientCommandListener");
		if (commandListener != null) {
			commandListener.processInput(userInput);
		} //else System.out.println("cL null");
	}
	
	/**
	 * shutdown the Client and close all streams and sockets
	 */
	public void shutdown() {
		try {
			//udpSocket.shutdown();
			socket.close();
			in.close();
		} catch (IOException e) {
			// in is already closed or not opened
		} catch (NullPointerException e) {
			// socket is already closed
		}
	}
	
	/**
	 * for testing purpose- to mute the client
	 * @param testingClient
	 
	private void setTestingClient(boolean testingClient) {
		this.isTestingClient = testingClient;
	}
	*/
}
