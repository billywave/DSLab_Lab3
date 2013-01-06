package client;

public class OnlineUser {
	private String ip = "";
	private int port = 0;
	private String name;
	
	public OnlineUser(String ip, int port, String name) {
		this.ip = ip;
		this.port = port;
		this.name = name;
	}
	
	public String getIP() {
		return ip;
	}
	
	public int getPort() {
		return port;
	}
	
	public String getName() {
		return name;
	}
}
