package security;

import java.io.IOException;

public interface Channel {
	public String readLine() throws IOException;
	public void close();
	public void flush();
	public void println(String line);
	/**
	 * Appends a new line to an input stream stack
	 * which will be returned on the next call of readLine()
	 * @param line 
	 */
	//public void appendToInputStream(String line);
}
