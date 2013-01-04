package security;

import java.io.IOException;
import java.util.Arrays;
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
		return new String(this.readBytes());
	}
	
	//@Override
	public byte[] readBytes() throws IOException {
		String line = channel.readLine();
		if (line == null) return null;
		
		// Used to be able to read unencrypted messages through the encryption layers (decorators)
		if (!encodedRead) {
			logger.debug("B64 not encoded read");
			return line.getBytes();
		}
		
		// pass !list command undecrypted
		if (line.equals("!list")) return line.getBytes();
		
		return decode(line.getBytes());
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
		this.printBytes(line.getBytes());
	}
	
	//@Override
	public void printBytes(byte[] line) {
		channel.println(new String(encode(line)));
	}
	
	/*
	 * Decodes a given line in Base64
	 */
	private byte[] decode(byte[] messageLine) {
		if (messageLine == null) return null;
		return Base64.decode(messageLine);
	}
	
	/*
	 * Encodes a given line to Base64
	 */
	private byte[] encode(byte[] messageLine) {
		if (messageLine == null) return null;
		return Base64.encode(messageLine);
	}
}
