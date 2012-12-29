package security;

import java.io.IOException;

public interface Channel {
	public String readLine() throws IOException;
	public void close();
	public void flush();
	public void print(String line);
	public void println(String line);
	
}
