package administrationObjects;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Bill implements Serializable {
    
	private static final long serialVersionUID = 4147417665626639875L;
	private Map<Long, Bill.Entry> entries;
    
    public Bill() {
        entries = Collections.synchronizedMap(new HashMap<Long, Bill.Entry>());
    }
    
    /**
     * Adds an entry for a finished auction to the bill
     * @param auctionID
     * @param price 
     */
    public void billAuction(long auctionID, double price, double fixedFee, double variableFee) {
        entries.put(auctionID, new Bill.Entry(auctionID, price, fixedFee, variableFee));
    }
    
    @Override
    public String toString() {
        String bill = "auction_ID strike_price    fee_fixed fee_variable    fee_total";
        for (Entry entry : entries.values()) {
            bill += String.format("\n%10d%13.2f%13.2f%13.2f%13.2f", entry.getId(), 
                                   entry.getPrice(), entry.getFixedFee(), 
                                   entry.getVariableFee(), entry.getFixedFee()+(entry.getPrice()*entry.getVariableFee())/100);
        }
        return bill;
    }
    
    /**
     * Subclass for the entries of the bill
     */
    private class Entry implements Serializable {
		private static final long serialVersionUID = -2350562383583660153L;
		private Long id;
        private double price;
        private double fixedFee;
        private double variableFee;
        
        public Entry(Long id, double price, double fixedFee, double variableFee) {
            this.id = id;
            this.price = price;
            this.fixedFee = fixedFee;
            this.variableFee = variableFee;
        }
        
        public Long getId() {
            return this.id;
        }
        
        public double getPrice() {
            return this.price;
        }
        
        public double getFixedFee() {
            return this.fixedFee;
        }
        
        public double getVariableFee() {
            return this.variableFee;
        }
    }
}
