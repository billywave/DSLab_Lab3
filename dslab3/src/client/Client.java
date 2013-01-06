package client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import loadTestingComponent.LoadTestClient;

import org.apache.log4j.Logger;

import security.Channel;
import security.SecureClientChannel;

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
	
	List<OnlineUser> onlineUsers = new ArrayList<OnlineUser>();
	
	boolean ioNotFound = false;
	boolean isTestingClient = false;
	
	public Socket socket = null;
    //BufferedReader in = null;
	Channel serverChannel;
    //UDPSocket udpSocket;
    
    ClientCommandListener commandListener;
    Client_ServerSocket serverSocket;
    
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
			this.udpPort = Integer.parseInt(args[2]);	
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
            serverChannel = new SecureClientChannel(socket);
			
            //in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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
		commandListener = new ClientCommandListener(serverChannel, udpPort, this);
		serverSocket = new Client_ServerSocket(udpPort);
		Main_Client.clientExecutionService.execute(serverSocket);
		
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
    	    	while((incommingMessage = serverChannel.readLine()) != null) {
    	    		// server shut down
    	    		if (incommingMessage.equals("shutdownServer")) {
    	    			System.out.println("Sorry, the Server just went offline, you have been logged out automatically. " +
					"Please press ENTER to shut down your Client and try to start it again later!");
    	    			shutdown();
    	    			ClientCommandListener.shutdown();
    	    		}
    	    		else if (incommingMessage.startsWith("firstusers: ")) {
    	    			logger.debug("Obtained first client-list");
    	    			String[] userArray = incommingMessage.substring(12).split("\n");
    	    			
						synchronized (onlineUsers) {
							onlineUsers.clear();
							for (int i = 0; i < userArray.length; i++) {
								String[] inetAddress_name = userArray[i].split(" ");
								String name = inetAddress_name[2];
	    	    				logger.debug("name "+name);
								String[] ip_portArray = inetAddress_name[0].split(":");
								if (ip_portArray.length >= 2) {
//									logger.debug("saving: " + ip_portArray[0] + ":" + Integer.parseInt(ip_portArray[1]));
									onlineUsers.add(new OnlineUser(ip_portArray[0],Integer.parseInt(ip_portArray[1]), name));
								}
							}
						}
						logger.debug("currently are " + onlineUsers.size() + " users online");
    	    			incommingMessage = "";
    	    		}
    	    		else if (incommingMessage.startsWith("users: ")) {
    	    			synchronized (onlineUsers) {
    	    				onlineUsers.clear();
	    	    			String[] userArray = incommingMessage.substring(7).split("\n");
	    	    			for (int i = 0; i < userArray.length; i++) {
	    	    				String[] inetAddress_name = userArray[i].split(" ");
	    	    				String name = inetAddress_name[2];
	    	    				logger.debug("name "+name);
								String[] ip_portArray = inetAddress_name[0].split(":");
								if (ip_portArray.length >= 2) {
//									logger.debug("saving: " + ip_portArray[0] + ":" + Integer.parseInt(ip_portArray[1]));
									onlineUsers.add(new OnlineUser(ip_portArray[0],Integer.parseInt(ip_portArray[1]), name));
								}
	    	    			}
	    	    			logger.debug("currently are " + onlineUsers.size() + " users online");
	    	    			System.out.println(incommingMessage.substring(7));
    	    			}
    	    			incommingMessage = "";
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
    	    				incommingMessage = "";
    	    			}
    	    			
    	    		}
    			}
    		} catch (IOException e) {
    			// empty- just is thrown when ending client
    		} catch (NullPointerException e) {
    			logger.info("The Auctionserver just went offline. You have been logged out automatically.");
    			
    			boolean auctionServerIsOnline = false; // for Client itself
    			commandListener.serverIsOnline = false; // for comand listener- to catch interactive commands
    			
    			while (!auctionServerIsOnline) {
    				
    				try {
        				synchronized(this) {
        					wait(1000);
        					try {
        						socket = new Socket(host, tcpPort);
        						serverChannel = new SecureClientChannel(socket);
        						auctionServerIsOnline = true;
        						commandListener.setServerChannel(serverChannel);
        						logger.info("The Aucitonserver just went online again. Please log in again!");
        						listenToAuctionServer();
        					} catch (UnknownHostException e1) {
        						// do not display every try.
        					} catch (IOException e1) {
        						// do not display every try.
        					}
        				}
        				
        			} catch (InterruptedException e1) {
        				logger.info("got interrupted");
        			} 
    			}
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
			serverChannel.close();
		} catch (IOException e) {
			// in is already closed or not opened
		} catch (NullPointerException e) {
			// socket is already closed
		}
	}
	
	public synchronized List<OnlineUser> getOnlineUsers() {
		return onlineUsers;
	}
	
	/**
	 * for testing purpose- to mute the client
	 * @param testingClient
	 
	private void setTestingClient(boolean testingClient) {
		this.isTestingClient = testingClient;
	}
	*/
}
