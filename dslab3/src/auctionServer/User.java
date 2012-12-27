package auctionServer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class User {

	private String name = "";
	private boolean online = false;
	private InetAddress inetAdress;
	private int tcpPort;
	private int udpPort;
	
	// list for offline- notifications
	List<String> syncNotificationsList = Collections.synchronizedList(new ArrayList<String>());
	
	public User(String name) {
		this.name = name;
	}
	
	/**
	 * constructor 
	 * 
	 * @param name
	 * @param online
	 * @param inetAddress
	 * @param port
	 * @param udpPort
	 */
	public User(String name, boolean online, InetAddress inetAddress, int port, int udpPort) {
		this.name = name;
		this.online = online;
		this.inetAdress = inetAddress;
		this.tcpPort = port;
		this.udpPort = udpPort;
	}
	
	public void storeNotification(String notification) {
		synchronized (syncNotificationsList) {
			syncNotificationsList.add(notification);
		}
	}
	
	public void sendNotifications() {
		synchronized (syncNotificationsList) {
			Iterator<String> iterator = syncNotificationsList.iterator();
			while (iterator.hasNext()) {
				String notification = iterator.next();
				try {
					AuctionServer_UDPSocket.getInstamce().sendMessage(inetAdress, udpPort, notification);
				} catch (IOException e) {
					System.out.println("Error: sending the stored Notifications failed");
				}
			}
			syncNotificationsList.clear();
		}
		
	}
	
	public String getName() {
		return name;
	}
	public synchronized void setName(String name) {
		this.name = name;
	}
	public boolean isOnline() {
		return online;
	}
	public synchronized void setOnline(boolean online) {
		this.online = online;
	}
	public InetAddress getInternetAdress() {
		return inetAdress;
	}
	public synchronized void setInternetAdress(InetAddress inetAdress) {
		this.inetAdress = inetAdress;
	}
	public int getPort() {
		return tcpPort;
	}
	public synchronized void setPort(int port) {
		this.tcpPort = port;
	}
	public int getUdpPort() {
		return udpPort;
	}
	public synchronized void setUdpPort(int udpPort) {
		this.udpPort = udpPort;
	}
	
}
