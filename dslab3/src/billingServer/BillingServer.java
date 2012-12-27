package billingServer;

import java.io.IOException;
import java.rmi.ConnectException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import rmi_Interfaces.BillingServer_RO;

public class BillingServer {
    private static Logger logger = Logger.getLogger(BillingServer.class);
    
    private String registryHost = "";
    private int registryPort = 0;
    
    // class member to make unbinding easier
    private Registry registry;
    private BillingServerRMI_Impl billingServer;
    private String bindingName = "";
    
    /**
     * non static replace of the static main method
     * @param args 
     */
    public void startBillingServer(String args[]) {
        // Only has to be called once (maybe once for each server/client)
        PropertyConfigurator.configure("src/log4j.properties");        
        
        readRegistryProperties();
        
        // read args
        try {
            bindingName = args[0];
        } catch (NumberFormatException e) {
            logger.error("Failed to read args");
	} catch (IndexOutOfBoundsException e) {
            logger.error("Too few arguments");
	}
        
        
        // Connecting to the predefined registry host:port
        try {
            registry = LocateRegistry.getRegistry(registryHost, registryPort);
            try {
                registry.list();
                logger.info("Using registry on " + registryHost + ":" + registryPort);
            // if connection is refused, creating a new registry on the local host instead
            } catch (ConnectException ce) {
                registry = LocateRegistry.createRegistry(registryPort);
                logger.info("Registry not found on " + registryHost + ":" + registryPort
                            + ". Creating registry on the billing server");
            }
            
            // binding to the registry
            billingServer = new BillingServerRMI_Impl();
            BillingServer_RO billingServerIf = billingServer;
            BillingServer_RO billingServerStub = (BillingServer_RO) UnicastRemoteObject.exportObject(billingServerIf, 0);
            registry.rebind(bindingName, billingServerStub);
        } catch (RemoteException re) {
            logger.error("Billing Server failed to bind itself to the registry");
        }        
    }
    
    /**
     * Looks up and saves the registry host+port stored in the registry.properties file 
     */
    private void readRegistryProperties() {
        java.io.InputStream is = ClassLoader.getSystemResourceAsStream("registry.properties");
        if (is != null) {
            java.util.Properties props = new java.util.Properties();
            try {
                props.load(is);
                this.registryPort = Integer.parseInt(props.getProperty("registry.port"));
                this.registryHost = props.getProperty("registry.host");
                is.close();
            } catch (IOException e) {
                logger.error("Failed to open registry properties");
            }
        } else {
            logger.error("Registry properties file not found");
        }
    }
    
    /**
     * Invalidates the instanciated Remote Object and unbinds it from the registry
     * to allow the server to shutdown properly
     */
    public void shutdown() {
        try {
            registry.unbind(bindingName);
        } catch (RemoteException re) {
            logger.warn("Connection to the registry for unbinding failed");
        } catch (NotBoundException nbe) {
            logger.info("Billing Server already not bound to registry");
        }
        
        billingServer.unexportBillingServerSecure();
        
        try {
            UnicastRemoteObject.unexportObject(billingServer, true);
        } catch (NoSuchObjectException nsoe) {
            logger.warn("Unexporting failed: billing server object found");
        }
                
    }



    
}
