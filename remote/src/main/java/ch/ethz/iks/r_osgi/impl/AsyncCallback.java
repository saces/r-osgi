package ch.ethz.iks.r_osgi.impl;

import ch.ethz.iks.r_osgi.messages.RemoteOSGiMessage;

public interface AsyncCallback {

	void result(final RemoteOSGiMessage msg);
	
}
