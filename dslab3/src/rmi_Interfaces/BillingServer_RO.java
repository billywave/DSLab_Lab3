package rmi_Interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BillingServer_RO extends Remote {
    
    /**
     * Called by the managementClient
     * Logs a user into the system by checking username and password
     * @param username
     * @param password
     * @return Remote Obect Reference to the Secure Part of the Billing Server
     * @throws RemoteException 
     */
    BillingServerSecure_RO login(String username, String password) throws RemoteException;
    
}
