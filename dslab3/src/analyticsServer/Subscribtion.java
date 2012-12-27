package analyticsServer;

import java.util.regex.Pattern;

import rmi_Interfaces.EventListener_RO;

public class Subscribtion {

	private static int idInt = 0;
	private final String ID = Subscribtion.createID();
	private Pattern filterRegEx;
	private EventListener_RO eventListener;
	
	public Subscribtion(Pattern filterRegEx, EventListener_RO eventListener) {
		this.filterRegEx = filterRegEx;
		this.eventListener = eventListener;
	}
	
	public Pattern getFilterRegEx() {
		return filterRegEx;
	}

	public synchronized void setFilterRegEx(Pattern filterRegEx) {
		this.filterRegEx = filterRegEx;
	}

	public EventListener_RO getEventListener() {
		return eventListener;
	}

	public synchronized void setEventListener(EventListener_RO eventListener) {
		this.eventListener = eventListener;
	}

	public String getID() {
		return ID;
	}
	
	/**
	 * create a unique ID (which is an increasing integer)
	 * @return ID
	 */
	private static synchronized String createID() {
		return Integer.toString(idInt++);
	}
}
