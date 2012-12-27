package analyticsServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

public class AnalyticsServerCmdListener implements Runnable {

	private Logger logger = Logger.getLogger(AnalyticsServerCmdListener.class);
	
	AnalyticsServer analServer = null;
	BufferedReader stdIn = null;
	
	public AnalyticsServerCmdListener(AnalyticsServer analServer) {
		this.analServer = analServer;
	}
	
	@Override
	public void run() {
		
		// Reading from the system shell/cmd input
        stdIn = new BufferedReader(new InputStreamReader(System.in));
        String userInput;
        try {
			while ((userInput = stdIn.readLine()) != null) {
				
				// shut dwon if input equals "!exit".
				if (userInput.equals("!exit")) {
					shutdown();
					break;
				}
			}
		} catch (IOException e) {
			logger.error("couldn't read I/O");
		}
	}
	
	private void shutdown() {
		try {
			stdIn.close();
		} catch (IOException e) {
			logger.error("couldn't close BufferedReader!");
		}
		analServer.shutdown();
		logger.debug("shutdown complete.");
	}

}
