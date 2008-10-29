package ch.ethz.iks.r_osgi.sample.service;

import ch.ethz.iks.r_osgi.sample.api.ServiceInterface;
import ch.ethz.iks.r_osgi.sample.dependency.Message;

public abstract class SmartService implements ServiceInterface {

	public abstract String echoService(String message, Integer count);

	public String reverseService(String message) {
		Message msg = new Message(message);
		return msg.reverse();
	}

	public abstract boolean equalsRemote(Object other);

	public void local() {
		System.out.println("Local invocation");
		zero();
	}

	public abstract void printRemote(int i, float f);

	public abstract void zero();

	public boolean equals(Object obj) {
		System.out.println("checking for equality");
		if (obj == null) {
			return false;
		} else {
			return ( !obj.equals(this) ) ? equalsRemote(obj) : true;
		}
	}

}
