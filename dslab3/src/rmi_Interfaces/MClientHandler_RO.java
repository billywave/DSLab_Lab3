package rmi_Interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

import event.Event;

public interface MClientHandler_RO extends Remote {
	
	/**
	 * Will be called from the ManagementClient to suscribe for Events
	 * To receive nutifications, the client sends a referenc to itself as a 
	 * remote Object.
	 * The eventRegEx expresses for which types of events the client wants to 
	 * suscribe. 
	 * 
	 * @param eventListener
	 * @param eventRegEx
	 * @return ID of the suscribtion
	 * @throws RemoteException
	 */
	String suscribe(EventListener_RO eventListener, String eventRegEx) throws RemoteException;
	
	/**
	 * Will be called from the ManagementClient to
	 * end a suscribtion with the suscribtionID
	 * 
	 * @param suscribtionID
	 */
	String unsuscribe(EventListener_RO eventListener, String suscribtionID)  throws RemoteException ;
	
	/**
	 * will be called from the Analytics Server 
	 * 
	 * @param event
	 */
	void processEvent(Event event)  throws RemoteException ;
}
