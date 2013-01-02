package security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Stack;
import org.apache.log4j.Logger;

/**
 *
 * @author Bernhard
 */
public class TCPChannel implements Channel{
	private Logger logger = Logger.getLogger(this.getClass());
	
	private PrintWriter out = null;
	private BufferedReader in = null;
	
	private Stack<String> lineBuffer = new Stack<String>();
	
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
		if (!lineBuffer.empty()) return lineBuffer.pop();
		return in.readLine();
	}

	@Override
	public void println(String line) {
		out.println(line);
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
		} catch (IOException ex) {
			//ex.printStackTrace();
		}
	}

	/**
	 * Appends a new line to an input stream stack
	 * which will be returned on the next call of readLine()
	 * @param line 
	 */
	@Override
	public void appendToInputStream(String line) {
		lineBuffer.push(line);
	}

}
