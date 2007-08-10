package ch.ethz.iks.r_osgi.impl;

import java.util.Dictionary;
import java.util.List;

import org.osgi.framework.ServiceRegistration;

import ch.ethz.iks.r_osgi.RemoteOSGiException;
import ch.ethz.iks.r_osgi.RemoteOSGiMessage;
import ch.ethz.iks.r_osgi.channels.ChannelEndpoint;

class EndpointMultiplexer implements ChannelEndpoint {

	final static int NONE = 0;

	final static int FAILOVER_REDUNDANCY = 1;

	final static int LOADBALANCING_ANY = 2;

	final static int LOADBALANCING_ONE = 3;

	private ChannelEndpoint primary;

	private int policy;

	private List endpoints;
	
	private String myURL;

	private ServiceRegistration reg;

	EndpointMultiplexer(final ChannelEndpoint primary) {
		this.primary = primary;
		this.policy = NONE;
		myURL = primary.getURL();
	}

	public void setPolicy(int policy) {
		this.policy = policy;
	}

	void addEndpoint(ChannelEndpoint endpoint) {
		endpoints.add(endpoint);
	}

	void removeEndpoint(ChannelEndpoint endpoint) {
		endpoints.remove(endpoint);
	}

	public void dispose() {
		throw new IllegalArgumentException(
				"Not supported through endpoint multiplexer");
	}

	public Dictionary getPresentationProperties(String serviceURL) {
		return primary.getPresentationProperties(serviceURL);
	}

	public Dictionary getProperties(String serviceURL) {
		return primary.getProperties(serviceURL);
	}

	public String getURL() {
		return myURL;
	}

	public Object invokeMethod(String serviceURL, String methodSignature,
			Object[] args) throws Throwable {
		if (policy == LOADBALANCING_ANY) {
			// TODO: do the load balancing
			return primary.invokeMethod(serviceURL, methodSignature, args);
		} else {
			try {
				return primary.invokeMethod(serviceURL, methodSignature, args);
			} catch (RemoteOSGiException e) {
				if (policy == FAILOVER_REDUNDANCY && !endpoints.isEmpty()) {
					// do the failover
					primary.untrackRegistration(myURL);
					primary = (ChannelEndpoint) endpoints.remove(0);
					primary.trackRegistration(myURL, reg);
					return invokeMethod(serviceURL, methodSignature, args);
				} else {
					throw e;
				}
			}
		}

	}

	public void receivedMessage(RemoteOSGiMessage msg) {
		throw new IllegalArgumentException(
				"Not supported through endpoint multiplexer");
	}

	public void trackRegistration(String serviceURL, ServiceRegistration reg) {
		this.reg = reg;
		primary.trackRegistration(serviceURL, reg);
	}

	public void untrackRegistration(String serviceURL) {
		primary.untrackRegistration(serviceURL);
	}

}
