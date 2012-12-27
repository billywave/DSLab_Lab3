package rmi_Interfaces;

import administrationObjects.Bill;
import administrationObjects.PriceSteps;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BillingServerSecure_RO extends Remote {
    
    /**
     * 
     * @return
     * @throws RemoteException 
     */
    PriceSteps getPriceSteps() throws RemoteException;
    
    /**
     * 
     * @param startPrice
     * @param endPrice
     * @param fixedPrice
     * @param variablePricePercent
     * @throws RemoteException 
     */
    void createPriceStep(double startPrice, double endPrice, 
                         double fixedPrice, double variablePricePercent) throws RemoteException;
    
    /**
     * 
     * @param startPrice
     * @param endPrice
     * @throws RemoteException 
     */
    void deletePriceStep(double startPrice, double endPrice) throws RemoteException;
    
    /**
     * 
     * @param user
     * @param auctionID
     * @param price
     * @throws RemoteException 
     */
    void billAuction(String user, long auctionID, double price) throws RemoteException;
    
    /**
     * 
     * @param user
     * @return
     * @throws RemoteException 
     */
    Bill getBill(String user) throws RemoteException;
    
}
