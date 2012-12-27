package managementClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManagementClient {
    MClientCmdListener cmdListener;

    String analyticsServerRef = "";
	String billingServerRef = "";
        
	ExecutorService mClientExecutionService;
	
	public ManagementClient(String args[]) {
		// read args
		try {
			analyticsServerRef = args[0];
			billingServerRef = args[1];
		} catch (IndexOutOfBoundsException e) {
			System.out.println("Error: args- array to short!");
		} catch (NumberFormatException e) {
			System.out.println("Error: format error in args- array!");
		}
	}
	
	public ManagementClient(String analyticServerRef) {
		this.analyticsServerRef = analyticServerRef;
	}
	
	public void start() {
		mClientExecutionService = Executors.newCachedThreadPool();
		
		cmdListener = new MClientCmdListener(analyticsServerRef, billingServerRef, mClientExecutionService);
		mClientExecutionService.execute(cmdListener);
	}
        
        /**
         * Should be called from static Main_ManagementClient.shutdown();
         */
        public void shutdown() {
            cmdListener.shutdown();
            mClientExecutionService.shutdown();
        }
	
        /**
         * Process input from external source
         * Called from testing component
         * @param userInput
         */
        public void processInput(String userInput) {
        	cmdListener.processInput(userInput);
        }
}
