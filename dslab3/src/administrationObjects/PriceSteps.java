package administrationObjects;

import exceptions.PriceStepIntervalCollisionException;
import exceptions.PriceStepIntervalNotFoundException;
import exceptions.PriceStepNegativeArgumentException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PriceSteps implements Serializable {

	private static final long serialVersionUID = 1846724687599757571L;
	private List<PriceStep> priceSteps;

	public PriceSteps() {
		priceSteps = Collections.synchronizedList(new ArrayList<PriceStep>());
	}

	/**
	 * Adds a new PriceStep to the PriceStep configuration
	 *
	 * @param StartPrice
	 * @param endPrice
	 * @param fixedPrice
	 * @param variablePricePercent
	 */
	public void addPriceStep(double startPrice, double endPrice, double fixedPrice,
			double variablePricePercent) throws RemoteException {
		
		// trimming to 2 decimal points
		startPrice = round(startPrice);
		endPrice = round(endPrice);
		fixedPrice = round(fixedPrice);
		variablePricePercent = round(variablePricePercent);
		

		PriceStep newStep = new PriceSteps.PriceStep(startPrice, endPrice, fixedPrice, variablePricePercent);
		
		// Checking for positive arguments
		if (startPrice < 0 || endPrice < 0 || fixedPrice < 0 || variablePricePercent < 0) {
			throw new PriceStepNegativeArgumentException();
		}
		
		// check for overlapping with stored pricesteps
		for (PriceStep storedStep : priceSteps) {
			if (newStep.overlaps(storedStep)) throw new PriceStepIntervalCollisionException();
		}
		
		this.priceSteps.add(newStep);
	}

	/**
	 * Removes a pricestep from the pricestep configuration
	 *
	 * @param startPrice
	 * @param endPrice
	 */
	public void removePriceStep(double startPrice, double endPrice) throws RemoteException {
		// trimming to 2 decimal points
		startPrice = round(startPrice);
		endPrice = round(endPrice);
		
		// Checking for positive arguments
		if (startPrice < 0 || endPrice < 0) throw new PriceStepNegativeArgumentException();
		
		PriceStep removeStep = null;
		for (PriceStep storedStep : priceSteps) {
			if (storedStep.getStartPrice() == startPrice && storedStep.getEndPrice() == endPrice) {
				removeStep = storedStep;
			}
		}
		if (removeStep == null) {
			throw new PriceStepIntervalNotFoundException();
		} else {
			priceSteps.remove(removeStep);
		}
	}

	public double getFixedFee(double price) {
		for (PriceStep storedStep : priceSteps) {
			if (price > storedStep.getStartPrice() && (storedStep.getEndPrice() == 0 || price <= storedStep.getEndPrice())) {
				return storedStep.getFixedPrice();
			}
		}
		return 0d;
	}

	public double getVariableFee(double price) {
		for (PriceStep storedStep : priceSteps) {
			if (price > storedStep.getStartPrice() && (storedStep.getEndPrice() == 0 || price <= storedStep.getEndPrice())) {
				return storedStep.getVariablePricePercent();
			}
		}
		return 0d;
	}
	
	private double round(double highPrecision) {
		BigDecimal bd = new BigDecimal(highPrecision);
		BigDecimal lowPrecision = bd.setScale(2, BigDecimal.ROUND_HALF_DOWN);
		return lowPrecision.doubleValue();
	}

	/**
	 * Returns a set of pricesteps in a table format
	 */
	@Override
	public String toString() {
		String steps = "      Min_Price      Max_Price      Fee_Fixed   Fee_Variable";
		for (PriceStep storedStep : priceSteps) {
			if (storedStep.endPrice == 0) {
				steps += String.format("\n%15.2f       INFINITY%15.2f%14.2f", storedStep.startPrice, storedStep.fixedPrice, storedStep.variablePricePercent) + "%";
			} else {
			steps += String.format("\n%15.2f%15.2f%15.2f%14.2f", storedStep.startPrice, storedStep.endPrice, storedStep.fixedPrice, storedStep.variablePricePercent) + "%";
			}
		}
		return steps;
	}

	/**
	 * Subclass for a single PriceStep
	 */
	private class PriceStep implements Serializable {

		private static final long serialVersionUID = 4680848550687262405L;
		// TODO lab3: store as bigdecimal internally
		private double startPrice;
		private double endPrice;
		private double fixedPrice;
		private double variablePricePercent;

		public PriceStep(double startPrice, double endPrice, double fixedPrice, double variablePricePercent) {
			this.startPrice = startPrice;
			this.endPrice = endPrice;
			this.fixedPrice = fixedPrice;
			this.variablePricePercent = variablePricePercent;
		}

		/**
		 * Checks if two PriceSteps' price ranges overlap each other
		 *
		 * @param other
		 * @return true if overlap
		 */
		public boolean overlaps(PriceStep other) {
			if (this.getEndPrice() == 0 && other.getEndPrice() == 0) {
				return true;
			}
			if ((this.getEndPrice() != 0 && other.getStartPrice() >= this.getEndPrice())
					|| (other.getEndPrice() != 0 && other.getEndPrice() <= this.getStartPrice())) {
				return false;
			} else {
				return true;
			}
		}

		/**
		 * @return the startPrice
		 */
		public double getStartPrice() {
			return startPrice;
		}

		/**
		 * @return the endPrice
		 */
		public double getEndPrice() {
			return endPrice;
		}

		/**
		 * @return the fixedPrice
		 */
		public double getFixedPrice() {
			return fixedPrice;
		}

		/**
		 * @return the variablePricePercent
		 */
		public double getVariablePricePercent() {
			return variablePricePercent;
		}
	}
}
