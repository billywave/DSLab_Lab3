package billingServer;

public class Main_BillingServer {
    
    private static BillingServerCmdListener cmdListener;
    private static BillingServer billingServer;

    /**
     * @param args
     */
    public static void main(String[] args) {
        
        cmdListener = new BillingServerCmdListener();
        cmdListener.start();
        
        billingServer = new BillingServer();
        billingServer.startBillingServer(args);
    }
    
    /**
     * Shuts down the billing server
     */
    public static void shutdown() {
        billingServer.shutdown();
    }
}
