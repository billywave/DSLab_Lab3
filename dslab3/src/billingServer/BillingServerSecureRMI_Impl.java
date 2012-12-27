package billingServer;

import administrationObjects.Bill;
import administrationObjects.PriceSteps;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import rmi_Interfaces.BillingServerSecure_RO;

public class BillingServerSecureRMI_Impl implements BillingServerSecure_RO {

    private static PriceSteps priceSteps;
    private static Map<String, Bill> bills;

    public BillingServerSecureRMI_Impl() {
        priceSteps = new PriceSteps();
        bills = Collections.synchronizedMap(new HashMap<String, Bill>());
    }

    @Override
    public PriceSteps getPriceSteps() throws RemoteException {
        return BillingServerSecureRMI_Impl.priceSteps;
    }

    @Override
    public void createPriceStep(double startPrice, double endPrice,
            double fixedPrice, double variablePricePercent) throws RemoteException {
        priceSteps.addPriceStep(startPrice, endPrice, fixedPrice, variablePricePercent);
    }

    @Override
    public void deletePriceStep(double startPrice, double endPrice) throws RemoteException {
        priceSteps.removePriceStep(startPrice, endPrice);
    }

    @Override
    public void billAuction(String user, long auctionID, double price) throws RemoteException {
        Bill bill;
        if (bills.containsKey(user)) {
            bill = bills.get(user);
        } else {
            bill = new Bill();
            bills.put(user, bill);
        }
        bill.billAuction(auctionID, price, priceSteps.getFixedFee(price), priceSteps.getVariableFee(price));
        
    }

    @Override
    public Bill getBill(String user) throws RemoteException {
        return bills.get(user);
    }
}
