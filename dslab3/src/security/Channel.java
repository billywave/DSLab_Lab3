package security;

import java.io.IOException;

public interface Channel {
	public String readLine() throws IOException;
	//public byte[] readBytes() throws IOException;
	public void close();
	public void flush();
	public void println(String line);
	//public void printBytes(byte[] line);
}
