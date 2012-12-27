package auctionServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
/**
 * UDP Socket following the Singelton pattern
 * -> there is only one static Object (Object == Class)!
 * 
 * @author Alexander Tatowsky
 *
 */
public class AuctionServer_UDPSocket {
	
	static AuctionServer_UDPSocket udpSocket;
	DatagramSocket datagramSocket = null;

	public AuctionServer_UDPSocket() {
		try {
			datagramSocket = new DatagramSocket();
		} catch (SocketException e) {
			System.out.println("Error: Socket creation failed!");
		}
	}
	
	/**
	 * send a message to a receiver with given address and port. 
	 * 
	 * @param receiverAddress
	 * @param udpPort
	 * @param msg
	 * @throws IOException
	 */
	public void sendMessage(InetAddress receiverAddress, int udpPort, String msg) throws IOException {
		byte[] buffer = msg.getBytes();

		DatagramPacket packet = new DatagramPacket(
		        buffer, buffer.length, receiverAddress, udpPort);
		datagramSocket.send(packet);
	}
	
	/**
	 * Singelton 
	 * @return static instance of the UDPSocket
	 */
	public static AuctionServer_UDPSocket getInstamce() {
		if (udpSocket == null) {
			return udpSocket = new AuctionServer_UDPSocket();
		} else {
			return udpSocket;
		}
	}
	
}
