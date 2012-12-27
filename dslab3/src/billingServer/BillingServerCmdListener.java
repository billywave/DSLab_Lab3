package billingServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.log4j.Logger;

public class BillingServerCmdListener extends Thread {
    private Logger logger = Logger.getLogger(BillingServerCmdListener.class);

    @Override
    public void run() {
        logger.info("Starting Billing Server Command Listener");
        
        // Reading from the system shell/cmd input
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(System.in));
        String inputLine;
        try {
            while ((inputLine = stdInput.readLine()) != null && !inputLine.equals("!exit")) {}
        } catch (IOException ex) {
            logger.error("System input stream disconnected");
        }
        
        Main_BillingServer.shutdown();
        logger.info("Closing Billing Server Command Listener");
    }

}
