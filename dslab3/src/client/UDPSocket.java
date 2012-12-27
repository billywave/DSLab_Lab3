package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPSocket implements Runnable {

	private int port = 0;
	static String username = "";
	DatagramSocket datagramSocket;
	private boolean exit = false;
	String[] stringParts;
	
	public UDPSocket(int port) {
		this.port = port;
		init();
	}
	
	private void init() {
		try {
			datagramSocket = new DatagramSocket(port);
		} catch (SocketException e) {
			System.out.println("Error: Client- Socket cannot be instancated!");
		}
	}
	
	/**
	 * listen to UDP- packets
	 */
	@Override
	public void run() {
		while (!exit) {
			byte[] buf = new byte[265];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			try {
				datagramSocket.receive(packet);
				String received = new String(packet.getData());
				processPacket(received.trim());
			} catch (IOException e) {
				// empty just is thrown when exiting
			}
		}
	}

	/**
	 * process packets by interpreting the string- stream
	 * 
	 * @param received
	 */
	public void processPacket(String received) {
		stringParts = received.split(" ");
		try {
			// auction- ended- notification
			if (stringParts[0].equals("!auction-ended")) {
				String describtion = "";
				
				// user is the highest bidder
				if (username.equals(stringParts[1].trim())) {
					for (int i = 3; i < stringParts.length; i++) {
						describtion += stringParts[i] + " ";
					}
					System.out.println("The auction '" + describtion +
							"' has ended. You won with " + stringParts[2] + "!");
				}
				else {
					// user is the owner- no one bid
					if (stringParts[1].equals("none")) {     // nobothy bid to this auction
						for (int i = 3; i < stringParts.length; i++) {
							describtion += stringParts[i] + " ";
						}
						System.out.println("The auction '" + describtion + "' has ended. " + 
								"Nobothy bid.");
					}
					// user is the woner and someone bid
					else {
						for (int i = 3; i < stringParts.length; i++) {
							describtion += stringParts[i] + " ";
						}
						System.out.println("The auction ' " + describtion + "' has ended. " + 
								stringParts[1] + " won with " + stringParts[2] + ".");
					}
				}
			}
			// new bid- notification
			if (stringParts[0].equals("!new-bid")) {
				String describtion = "";
				
				for (int i = 1; i < stringParts.length; i++) {
					describtion += stringParts[i] + " ";
				}
				System.out.println("You have been overbid on '" + describtion + "'.");
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Error: message not correct!");
		}
	}
	
	/**
	 * shut down the UDP- listener
	 */
	public void shutdown() {
		datagramSocket.close();
		this.exit = true;
	}
}
