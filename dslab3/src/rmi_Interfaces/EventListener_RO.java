package rmi_Interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

import event.Event;

public interface EventListener_RO extends Remote {

	void processEvent(Event event) throws RemoteException;
	
}
