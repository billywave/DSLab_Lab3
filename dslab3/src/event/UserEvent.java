package event;

import exceptions.WrongEventTypeException;

public class UserEvent extends Event {

	/**
	 * because of serialisazion
	 */
	private static final long serialVersionUID = 4436069348163783580L;

	public final static String USER_LOGIN = "USER_LOGIN";
	public final static String USER_LOGOUT = "USER_LOGOUT";
	public final static String USER_DISCONNECTED = "USER_DISCONNECTED";
	
	private String userName;
	
	public UserEvent(String type, long timestamp, String userName) throws WrongEventTypeException {
		if (!(type.equals(USER_LOGIN) || type.equals(USER_LOGOUT) || type.equals(USER_DISCONNECTED))) {
			throw new WrongEventTypeException();
		}
		this.type = type;
		this.timestamp = timestamp;
		this.userName = userName;
	}

	public String getUserName() {
		return userName;
	}

	public synchronized void setUserName(String userName) {
		this.userName = userName;
	}
}
