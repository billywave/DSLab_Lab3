package security;

import java.io.IOException;
import org.bouncycastle.util.encoders.Base64;


/**
 *
 * @author Bernhard
 */
public class Base64Channel implements Channel {
	private final Channel channel;

	public Base64Channel(Channel channel) {
		this.channel = channel;
	}

	@Override
	public String readLine() throws IOException {
		byte[] line = readBytes();
		if (line == null) return null;
		else return new String(line);
	}
	
	public byte[] readBytes() throws IOException {
		return decode(channel.readLine());
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
		else if (base64Line.length() == 0) return null;
		else {
			byte[] base64ByteArray = base64Line.getBytes();
			byte[] messageByteArray = Base64.decode(base64ByteArray);
			return messageByteArray;
		}
	}
	
	/*
	 * Encodes a given line to Base64
	 */
	private String encode(byte[] messageLine) {
		if (messageLine == null) return null;
		else {
			byte[] base64ByteArray = Base64.encode(messageLine);
			return new String(base64ByteArray);
		}
	}
	
	
}
