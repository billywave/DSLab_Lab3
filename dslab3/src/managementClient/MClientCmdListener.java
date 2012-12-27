package managementClient;

import administrationObjects.Bill;
import administrationObjects.PriceSteps;
import exceptions.PriceStepIntervalCollisionException;
import exceptions.PriceStepIntervalNotFoundException;
import exceptions.PriceStepNegativeArgumentException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.AccessException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import org.apache.log4j.Logger;
import rmi_Interfaces.BillingServerSecure_RO;
import rmi_Interfaces.BillingServer_RO;
import rmi_Interfaces.EventListener_RO;
import rmi_Interfaces.MClientHandler_RO;

public class MClientCmdListener implements Runnable {

	private static Logger logger = Logger.getLogger(MClientCmdListener.class);
	int registryPort;
	Registry registry;
	String registryHost;
	String analyticsServerRef;
	String billingServerRef;
	//String userInput;
	ExecutorService mClientExecutionService;
	MClientHandler_RO mClientHandler = null;
	EventListener_RO eventListener = null;
	BillingServer_RO billingServer = null;
	BillingServerSecure_RO billingServerSecure = null;

	public MClientCmdListener(String analyticsServerRef, String billingServerRef, ExecutorService mClientExecutionService) {
		this.analyticsServerRef = analyticsServerRef;
		this.billingServerRef = billingServerRef;
		this.mClientExecutionService = mClientExecutionService;
		
		// create EventListener Remote Object
		try {
			eventListener = new EventListener_Impl();
		} catch (RemoteException e1) {
			logger.error("EventListener Remote Exception");
		}
		
		readProperties();
		try {
			registry = LocateRegistry.getRegistry(registryHost, registryPort);
		} catch (RemoteException e1) {
			System.out.println("Couldn't find Registry!");
		}

		try {
			mClientHandler = (MClientHandler_RO) registry.lookup(analyticsServerRef);
		} catch (AccessException e1) {
			logger.error("Access to the registry denied");
		} catch (RemoteException e1) {
			System.out.println("Remote Objects from the Analytics Server cannot be found. For further usage please "
					+ "contact the support team and restart the client!");
		} catch (NotBoundException e1) {
			logger.error("Analytic Server not bound");
		}

		try {
			billingServer = (BillingServer_RO) registry.lookup(billingServerRef);
		} catch (AccessException e1) {
			logger.error("Access to the registry denied");
		} catch (RemoteException e1) {
			System.out.println("Remote Objects from the Billing Server cannot be found. For further usage please "
					+ "contact the support team and restart the client!");
		} catch (NotBoundException e1) {
			logger.error("Billing Server not bound");

		}
	}

	@Override
	public void run() {
		
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String userInput;
		try {
			while (((userInput = stdIn.readLine()) != null) && !userInput.equals("!exit")) {
				processInput(userInput);
			}
			Main_ManagementClient.shutdown();
		} catch (IOException e) {
			logger.error("System input stream disconnected");
		}

		
	}

	/**
	 * Processing the user input
	 *
	 * @param userInput
	 */
	protected void processInput(String userInput) {

		String[] splitInput = userInput.split(" ");

		// suscribe
		if (splitInput[0].equals("!subscribe")) {

			String filterRegEx = "";
			try {
				filterRegEx = splitInput[1];
			} catch (IndexOutOfBoundsException e) {
				System.out.println("Error: Please enter the log in command like this: !subscribe <filterRegex>:");
			}
			try {
				String answer;
				if (mClientHandler == null) {
					try {
						mClientHandler = (MClientHandler_RO) registry.lookup(analyticsServerRef);
					} catch (NotBoundException e) {
						logger.error("Analytic Server not bound");
					}
				}
				answer = mClientHandler.suscribe(eventListener, filterRegEx);

				System.out.println(answer);
			} catch (RemoteException e) {
				logger.error("Analytic Server is down, your command will be dropped");
			}
		}

		// unsuscribe
		if (splitInput[0].equals("!unsubscribe")) {

			String answer = "no answer...";

			// get id
			String id = "";
			try {
				id = splitInput[1];
			} catch (IndexOutOfBoundsException e) {
				System.out.println("Error: Please enter the unsuscribe command like this: !unsubscribe <ID>:");
			}

			try {
				answer = mClientHandler.unsuscribe(eventListener, id);
			} catch (RemoteException e) {
				logger.error("Analytic Server is down, your command will be dropped");
			}
			System.out.println(answer);
		}

		// print suscribtion states
		if (userInput.equals("!auto")) {
			System.out.println("auto- printing mode");
			byte state = 0;
			EventListener_Impl.setState(state);
		}
		if (userInput.equals("!hide")) {
			System.out.println("buffering events");
			byte state = 1;
			EventListener_Impl.setState(state);
		}
		if (userInput.equals("!print")) {
			EventListener_Impl.printBuffer();
		}

		// Berni
		//String splitInput[] = userInput.split(" ");
		//if (splitInput.length >= 1) {

		if (splitInput[0].equals("!login")) {
			try {
				String username = splitInput[1];
				String password = splitInput[2];
				this.login(username, password);
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println("Too few arguments");
			}
		} else if (splitInput[0].equals("!addStep")) {
			try {
				double startPrice = Double.parseDouble(splitInput[1]);
				double endPrice = Double.parseDouble(splitInput[2]);
				double fixedPrice = Double.parseDouble(splitInput[3]);
				double variablePricePercent = Double.parseDouble(splitInput[4]);
				this.addStep(startPrice, endPrice, fixedPrice, variablePricePercent);
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println("Too few arguments");
			} catch (NumberFormatException e) {
				System.out.println("Only numeric arguments allowed");
			}
		} else if (splitInput[0].equals("!removeStep")) {
			try {
				double startPrice = Double.parseDouble(splitInput[1]);
				double endPrice = Double.parseDouble(splitInput[2]);
				this.removeStep(startPrice, endPrice);
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println("Too few arguments");
			} catch (NumberFormatException e) {
				System.out.println("Only numeric arguments allowed");
			}
		} else if (splitInput[0].equals("!steps")) {
			this.steps();
		} else if (splitInput[0].equals("!bill")) {
			try {
				String username = splitInput[1];
				this.bill(username);
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println("Too few arguments");
			}

		} else if (splitInput[0].equals("!logout")) {
			this.logout();
		}
		//}

	}

	/**
	 * !login username password
	 *
	 * @param username
	 * @param password
	 */
	private void login(String username, String password) {
		if (billingServer == null) {
			System.out.println("ERROR: Not connected to the billing server");
		} else {
			try {
				BillingServerSecure_RO bss = billingServer.login(username, password);
				if (bss == null) {
					System.out.println("ERROR: Login failed");
				} else {
					billingServerSecure = bss;
					System.out.println(username + " successfully logged in");
				}
			} catch (RemoteException ex) {
				// maybe system.out.println :S
				logger.error("Billing Server Remote Exception");
			}
		}
	}

	/**
	 * !logout
	 */
	private void logout() {
		this.billingServerSecure = null;
	}

	/**
	 * !addStep startPrice endPrice fixedPrice variablePricePercent
	 *
	 * @param startPrice
	 * @param endPrice
	 * @param fixedPrice
	 * @param variablePricePercent
	 */
	private void addStep(double startPrice, double endPrice, double fixedPrice, double variablePricePercent) {
		if (billingServerSecure == null) {
			System.out.println("ERROR: You are currently not logged in");
		} // Checking for positive interval range
		else if (endPrice != 0 && startPrice >= endPrice) {
			System.out.println("ERROR: Negative or empty interval range");
		} else {
			try {
				billingServerSecure.createPriceStep(startPrice, endPrice, fixedPrice, variablePricePercent);
				System.out.println("Step [" + startPrice + " " + (endPrice == 0 ? "INFINITY" : endPrice) + "] successfully added");
			} catch (RemoteException ex) {
				if (ex.getCause() != null) {
					Throwable t = ex.getCause();
					if (t instanceof PriceStepIntervalCollisionException) {
						System.out.println("ERROR: PriceStep overlaps with existing PriceStep");
					} else if (t instanceof PriceStepNegativeArgumentException) {
						System.out.println("ERROR: Only positive arguments allowed");
					} else {
						logger.error("Billing Server Remote Exception");
					}
				} else {
					logger.error("Billing Server Remote Exception");
				}
			}
		}
	}

	/**
	 * !removeStep startPrice endPrice
	 *
	 * @param startPrice
	 * @param endPrice
	 */
	private void removeStep(double startPrice, double endPrice) {
		if (billingServerSecure == null) {
			System.out.println("ERROR: You are currently not logged in");
		} else {
			try {
				billingServerSecure.deletePriceStep(startPrice, endPrice);
				System.out.println("Step [" + startPrice + " " + (endPrice == 0 ? "INFINITY" : endPrice) + "] successfully removed");
			} catch (RemoteException ex) {
				if (ex.getCause() != null) {
					Throwable t = ex.getCause();
					if (t instanceof PriceStepIntervalNotFoundException) {
						System.out.println("ERROR: Price Step [" + startPrice + " " + (endPrice == 0 ? "INFINITY" : endPrice) + "] does not exist");
					} else if (t instanceof PriceStepNegativeArgumentException) {
						System.out.println("ERROR: Only positive arguments allowed");
					} else {
						logger.error("Billing Server Remote Exception");
					}
				} else {
					logger.error("Billing Server Remote Exception");
				}
			}
		}
	}

	/**
	 * !steps
	 */
	private void steps() {
		if (billingServerSecure == null) {
			System.out.println("ERROR: You are currently not logged in");
		} else {
			try {
				PriceSteps priceSteps = billingServerSecure.getPriceSteps();
				System.out.println(priceSteps);
			} catch (RemoteException ex) {
				logger.error("Billing Server Remote Exception");
			}
		}
	}

	/**
	 * !bill user
	 *
	 * @param user
	 */
	private void bill(String user) {
		if (billingServerSecure == null) {
			System.out.println("ERROR: You are currently not logged in");
		} else {
			try {
				Bill bill = billingServerSecure.getBill(user);
				if (bill == null) {
					System.out.println("ERROR: User not found or no bill yet");
				} else System.out.println(bill);
			} catch (RemoteException ex) {
				logger.error("Billing Server Remote Exception");
			}
		}
	}

	/**
	 * Reads the registry properties and stores the host and port values
	 */
	private void readProperties() {

		java.io.InputStream is = ClassLoader.getSystemResourceAsStream("registry.properties");
		if (is != null) {
			java.util.Properties props = new java.util.Properties();
			try {
				props.load(is);
				registryHost = props.getProperty("registry.host");
				registryPort = Integer.parseInt(props.getProperty("registry.port"));
			} catch (IOException e) {
				logger.error("Billing Server Remote Exception");
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					logger.error("Failed to close registry properties");
				}
			}
		} else {
			System.err.println("[Client1] Properties file not found!");
		}
	}

	/**
	 * Shuts down the management client 
	 * Should be called in MangementClient.shutdown()
	 */
	public void shutdown() {
		//logger.debug("shutdown() begin"); 
		
		try {
			UnicastRemoteObject.unexportObject(mClientHandler, true);
			logger.info("AnalyticServer remote object unexported"); 
		} catch (NoSuchObjectException nsoe) { 
			logger.warn("Unexporting failed: AnalyticServer remote object not found"); 
		}
		
		try { 
			UnicastRemoteObject.unexportObject(eventListener, true);
			logger.info("AnalyticServer Event Listener remote object unexported"); 
		} catch (NoSuchObjectException nsoe) {
			logger.warn("Unexporting failed: AnalyticServer Event Listener remote object not found"); 
		}
		
		try { 
			UnicastRemoteObject.unexportObject(billingServer, true);
			logger.info("BillingServer remote object unexported"); 
		} catch (NoSuchObjectException nsoe) { 
			logger.warn("Unexporting failed: BillingServer remote object not found"); 
		}
		
		try { 
			UnicastRemoteObject.unexportObject(billingServerSecure, true);
			logger.info("BillingServerSecure remote object unexported");
		} catch (NoSuchObjectException nsoe) { 
			logger.warn("Unexporting failed: BillingServerSecure remote object not found"); 
		}
		
		//logger.debug("shutdown() end"); 
		
	}
}
