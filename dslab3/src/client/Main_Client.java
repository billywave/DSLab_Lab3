package client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class is the entry class to the Client. It will start
 * the Client- the non- static- Object of the Client part.
 * 
 * Also the EcecutorService for Client Threads is contained 
 * in a static way that it is possible to close it from somwhere else.
 * 
 * @author Alexander Tatowsky
 *
 */
public class Main_Client {

	public static ExecutorService clientExecutionService = Executors.newCachedThreadPool();
	
	/**
	 * @param args containing:
	 * 
	 * args[0]: host: host name or IP of the auction server
	 * args[1]: TCP connection port on which the auction server is listening for incoming connections
	 * args[2]: this port will be used for instantiating a java.net.DatagramSocket
	 */
	public static void main(String[] args) {
		
		try {
			Client client = new Client(args);
			client.startClient();
		} catch (NumberFormatException e) {
			System.out.println("Something was wrong with the args[] arguments");
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Something was wrong with the args[] arguments");
		}
	}
}
