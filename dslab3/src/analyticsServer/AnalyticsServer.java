package analyticsServer;

import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.ConnectException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import rmi_Interfaces.MClientHandler_RO;

public class AnalyticsServer {

	private Logger logger = Logger.getLogger(AnalyticsServer.class);
	
	// thread pool- handler
	public ExecutorService analServerExecutionService;
	
	Registry registry;
	
	MClientHandler_RO mClientHandlerStub;
	MClientHandler_Impl mClientHandler;
	
	String registryHost = "";
	String bindingName = "";
	int registryPort = 0;
	
	public void startAnalyticsServer(String [] args) {
		
		// start Executorservice
		analServerExecutionService = Executors.newCachedThreadPool();
		
		AnalyticsServerCmdListener cmdListener = new AnalyticsServerCmdListener(this);
		analServerExecutionService.execute(cmdListener);
		
		// read args
		try {
			bindingName = args[0];
		} catch (NumberFormatException e) {
			logger.error("Error: Failed read args!");
		} catch (IndexOutOfBoundsException e) {
			logger.error("Error: args was to short!");
		}
		
		readProperties();
		mClientHandler = new MClientHandler_Impl();
		
		// create remote object and export it.
		try {
			mClientHandlerStub = (MClientHandler_RO) UnicastRemoteObject.exportObject(mClientHandler, 0);
		} catch (RemoteException e) {
			logger.error("couldn't export MClientHandler_RO");
		}
		try {
			registry = LocateRegistry.getRegistry(registryHost, registryPort);
            try {
                registry.list();
                logger.info("Using registry on " + registryHost + ":" + registryPort);
            // if connection is refused, creating a new registry on the local host instead
            } catch (ConnectException ce) {
                registry = LocateRegistry.createRegistry(registryPort);
                logger.info("Registry not found on " + registryHost + ":" + registryPort
                            + ". Creating registry on the analytics server");
            }
//			// bind the remote object to the registry
//			registry = LocateRegistry.createRegistry(registryPort);
			registry.rebind(bindingName, mClientHandlerStub);
		} catch (RemoteException e) {
			logger.error("couldn't bind the Registry to port" + registryPort);
		}
	}
	
	/**
	 * read the registry host and port from properties file
	 */
	private void readProperties() {
		java.io.InputStream is = ClassLoader.getSystemResourceAsStream("registry.properties");
		if (is != null) {
			java.util.Properties props = new java.util.Properties();
			try {
				try {
					props.load(is);
				} catch (IOException e) {
					logger.error("couldn't read Properties");
				}
				registryHost = props.getProperty("registry.host");
				registryPort = Integer.parseInt(props.getProperty("registry.port"));
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					logger.error("couldn't close InputStream");
				}
			}
		} else {
			logger.error("Properties file not found!");
		}
	}
	
	public void shutdown() {
		try {
			registry.unbind(bindingName);
		} catch (AccessException e) {
			logger.error("couldn't unbind the Registry");
		} catch (RemoteException e) {
			logger.error("couldn't unbind the Registry");
		} catch (NotBoundException e) {
			logger.error("couldn't unbind the Registry");
		}
		try {
			 UnicastRemoteObject.unexportObject(mClientHandler, true);
		} catch (NoSuchObjectException e) {
			logger.error("couldn't unexport the Stub");
		}
		
		// propper shutdown of the ExecutorService
		analServerExecutionService.shutdown(); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!analServerExecutionService.awaitTermination(3, TimeUnit.SECONDS)) {
				analServerExecutionService.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!analServerExecutionService.awaitTermination(3, TimeUnit.SECONDS))
					logger.error("Pool did not terminate");
				}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			analServerExecutionService.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
		logger.debug("shutdown complete.");
	}
}
