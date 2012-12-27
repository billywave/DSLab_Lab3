package managementClient;

import org.apache.log4j.PropertyConfigurator;

public class Main_ManagementClient {
        private static ManagementClient managementClient;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
                PropertyConfigurator.configure("src/log4j.properties");
		
		managementClient = new ManagementClient(args);
		managementClient.start();
	}
        
        /**
         * Shuts down the management client
         */
        public static void shutdown() {
            managementClient.shutdown();
        }
}
