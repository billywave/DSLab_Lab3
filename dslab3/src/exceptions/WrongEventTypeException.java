package exceptions;

public class WrongEventTypeException extends Exception {

	/**
	 * because of Serialization
	 */
	private static final long serialVersionUID = 1296157830972837783L;

	public WrongEventTypeException() {
		super("Event has an undefined EventType");
	}
	
}
