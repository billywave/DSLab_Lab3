package security;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;


/**
 *
 * @author Bernhard
 */
public class Base64Channel implements Channel {
	private Logger logger = Logger.getLogger(this.getClass());
	
	private final TCPChannel channel;

	private boolean encodedRead = true;
	
	public Base64Channel(TCPChannel channel) {
		this.channel = channel;
	}
	
	/**
	 * Sets the encode mode to true (default) or false to pass
	 * unencoded messages through the 
	 * @param encoded 
	 */
	protected void setEncodedRead(boolean encoded) {
		this.encodedRead = encoded;
	}

	@Override
	public String readLine() throws IOException {
		String lineString = channel.readLine();	
		if (lineString == null) return null;
//		{
//			logger.debug("B64 readLine A");
//			return null; //channel.readLine();
//		}
		logger.debug("B64 readLine B");
		return new String(decode(lineString));
	}
	
	public byte[] readBytes() throws IOException {
		String line = channel.readLine();
		// Used to be able to read unencrypted messages through the encryption layers (decorators)
		if (!encodedRead) {
			logger.debug("B64 not encoded read");
			return line.getBytes();
		}
		if (line == null) return null;
		return decode(line);
	}

	@Override
	public void close() {
		channel.close();
	}

	@Override
	public void flush() {
		channel.flush();
	}

	@Override
	public void println(String line) {
		channel.println(encode(line.getBytes()));
	}
	
	public void printBytes(byte[] line) {
		channel.println(encode(line));
	}
	
	/*
	 * Decodes a given line in Base64
	 */
	private byte[] decode(String base64Line) {
		if (base64Line == null) return null;
		if (base64Line.equals("!list")) return base64Line.getBytes();
		byte[] base64ByteArray = base64Line.getBytes();
		byte[] messageByteArray = Base64.decode(base64ByteArray);
		return messageByteArray;
	}
	
	/*
	 * Encodes a given line to Base64
	 */
	private String encode(byte[] messageLine) {
		if (messageLine == null) return null;
		byte[] base64ByteArray = Base64.encode(messageLine);
		return new String(base64ByteArray);
	}
}
