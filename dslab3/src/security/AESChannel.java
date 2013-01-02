package security;

import java.io.IOException;
import org.apache.log4j.Logger;

/**
 *
 * @author Bernhard
 */
public class AESChannel implements Channel {
	private Logger logger = Logger.getLogger(this.getClass());
	
	private final Channel channel;

	public AESChannel(Channel channel) {
		this.channel = channel;
	}

	@Override
	public String readLine() throws IOException {
		logger.debug("Next message received via AES");
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
		logger.debug("Previous message sent via AES");
		channel.println(line);
	}
	
	@Override
	public void appendToInputStream(String line) {
		channel.appendToInputStream(line);
	}
	
}
