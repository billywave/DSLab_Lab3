package security;

import java.io.IOException;

/**
 *
 * @author Bernhard
 */
public class RSAChannel implements Channel {
	private final Channel channel;

	public RSAChannel(Channel channel) {
		this.channel = channel;
	}

	@Override
	public String readLine() throws IOException {
		return channel.readLine();
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
		channel.println(line);
	}
	
}
