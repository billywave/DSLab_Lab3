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

	public Base64Channel(TCPChannel channel) {
		this.channel = channel;
	}

	@Override
	public String readLine() throws IOException {
		String lineString = channel.readLine();
		if (lineString == null) {
			logger.debug("B64 readLine A");
			return channel.readLine();
		}
		logger.debug("B64 readLine B");
		//byte[] line = readBytes();
		//if (line == null) return channel.readLine();
		return new String(decode(lineString));
	}
	
	public byte[] readBytes() throws IOException {
		String line = channel.readLine();
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
//		if (base64Line == null) return null;
//		else if (base64Line.length() == 0) return null;
//		else {
			byte[] base64ByteArray = base64Line.getBytes();
			byte[] messageByteArray = Base64.decode(base64ByteArray);
			return messageByteArray;
//		}
	}
	
	/*
	 * Encodes a given line to Base64
	 */
	private String encode(byte[] messageLine) {
//		if (messageLine == null) return null;
//		else {
			byte[] base64ByteArray = Base64.encode(messageLine);
			return new String(base64ByteArray);
//		}
	}

	@Override
	public void appendToInputStream(String line) {
		channel.appendToInputStream(encode(line.getBytes()));
	}
	
	public void appendToInputStream(byte[] line) {
		channel.appendToInputStream(encode(line));
	}
	
	
}
