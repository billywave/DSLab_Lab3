package event;

import java.io.Serializable;

public abstract class Event implements Serializable {

	/**
	 * because of Serializable
	 */
	private static final long serialVersionUID = 156065730386053603L;
	
	static int idInt;
	final String ID = createID();
	String type;
	long timestamp;
	
	public String getId() {
		return ID;
	}

	public String getType() {
		return type;
	}
	
	public synchronized void setType(String type) {
		this.type = type;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public synchronized void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public static synchronized String createID() {
		return Integer.toString(idInt++);
	}
}
