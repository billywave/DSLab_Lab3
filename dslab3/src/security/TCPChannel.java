package security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import org.apache.log4j.Logger;

/**
 *
 * @author Bernhard
 */
public class TCPChannel implements Channel{
	private Logger logger = Logger.getLogger(this.getClass());
	
	private PrintWriter out = null;
	private BufferedReader in = null;
	
	public TCPChannel(Socket socket) {
		try {
			out = new PrintWriter(socket.getOutputStream());
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			logger.error("Failed to bind input or output stream to user client");
		}
	}

	@Override
	public String readLine() throws IOException {
		return in.readLine();
	}
	
	//@Override
	public byte[] readBytes() throws IOException {
		return in.readLine().getBytes();
	}

	@Override
	public void println(String line) {
		out.println(line);
	}
	
	//@Override
	public void printBytes(byte[] line) {
		out.println(new String(line));
	}

	@Override
	public void flush() {
		out.flush();
	}

	@Override
	public void close() {
		out.close();
		try {
			in.close();
		} catch (IOException ex) {}
	}
}
