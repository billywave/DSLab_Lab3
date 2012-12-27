package billingServer;

import java.io.IOException;
import java.math.BigInteger;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;
import rmi_Interfaces.BillingServerSecure_RO;
import rmi_Interfaces.BillingServer_RO;

public class BillingServerRMI_Impl implements BillingServer_RO {
    private static Logger logger = Logger.getLogger(BillingServerRMI_Impl.class);
    
    private BillingServerSecure_RO billingServerSecure;
    BillingServerSecure_RO billingServerSecureStub;
            
    public BillingServerRMI_Impl() {
        try {
            billingServerSecure = new BillingServerSecureRMI_Impl();
            billingServerSecureStub = (BillingServerSecure_RO) UnicastRemoteObject.exportObject(billingServerSecure, 0);
        } catch (RemoteException e) {
            logger.error("Billing Server Secure Remote Exception");
        }
    }
    
    /**
     * Logs the user on to the billing server
     * @param username
     * @param password
     * @return Remote Object to the secure billing server, if login validates
     * @throws RemoteException 
     */
    @Override
    public BillingServerSecure_RO login(String username, String password) throws RemoteException {
        String passwordHash = getMd5Hash(password);
        String storedPasswordHash = getPassword(username);
        logger.debug("Entered hash: " + passwordHash);
        logger.debug("Stored hash for user " + username + ": " + storedPasswordHash);
        
        // comparing md5 hashes
        if (passwordHash != null && storedPasswordHash != null && 
            passwordHash.equals(storedPasswordHash)) {
            logger.info("User " + username + " successfully logs on to the server");
            return billingServerSecureStub;    
        } else {
            logger.debug("User " + username + " fails to log on to the server");
            return null;
        }
    }
    
    /**
     * Returns the stored md5 password hash for a given user
     * @param username
     * @return 
     */
    private String getPassword(String username) {
        String password = null;
        java.io.InputStream is = ClassLoader.getSystemResourceAsStream("user.properties");
        if (is != null) {
            java.util.Properties props = new java.util.Properties();
            try {
                props.load(is);
                if (props.containsKey(username)) password = props.getProperty(username);
                is.close();
            } catch (IOException e) {
                logger.error("Failed to open user properties");
            }
        } else {
            logger.error("User properties file not found");
        }
        return password;
    }
    
    /**
     * Returns the hex representation of the password's md5 hash
     * @param password
     * @return 
     */
    private String getMd5Hash(String password) {
        String hash = null;
        try {
            // creating md5 digest
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes());
            // turning the byte[] into a hexdecimal string
            hash = new BigInteger(1, md.digest()).toString(16);
        } catch (NoSuchAlgorithmException ex) {}
        return hash;
    }
    
    /**
     * Invalidates the instanciated Secure Remote Object
     */
    public void unexportBillingServerSecure() {
        try {
            UnicastRemoteObject.unexportObject(billingServerSecure, true);
        } catch (NoSuchObjectException nsoe) {
            logger.warn("Unexporting failed: billing server secure object found");
        }
                
    }
    
}
