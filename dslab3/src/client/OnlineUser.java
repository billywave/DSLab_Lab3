package client;

public class OnlineUser {
	private String ip = "";
	private int port = 0;
	
	public OnlineUser(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}
	
	public String getIP() {
		return ip;
	}
	
	public int getPort() {
		return port;
	}
}
