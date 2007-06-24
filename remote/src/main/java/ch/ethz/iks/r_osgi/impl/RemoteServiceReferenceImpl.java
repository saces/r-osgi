package ch.ethz.iks.r_osgi.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import ch.ethz.iks.r_osgi.RemoteServiceReference;

final class RemoteServiceReferenceImpl implements RemoteServiceReference {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1340731053890522970L;

	private String[] serviceInterfaces;

	private Dictionary properties;

	private String url;

	private transient ChannelEndpointImpl channel;

	RemoteServiceReferenceImpl(final String[] serviceInterfaces,
			final String url, final Dictionary properties,
			final ChannelEndpointImpl channel) {
		this.serviceInterfaces = serviceInterfaces;
		this.url = url;
		this.properties = properties;
		this.channel = channel;
	}

	public Object getProperty(final String key) {
		return properties.get(key);
	}

	public String[] getPropertyKeys() {
		final ArrayList result = new ArrayList(properties.size());
		for (Enumeration e = properties.keys(); e.hasMoreElements(); result
				.add((String) e.nextElement()))
			;
		return (String[]) result.toArray(new String[properties.size()]);
	}

	public String[] getServiceInterfaces() {
		return serviceInterfaces;
	}

	ChannelEndpointImpl getChannel() {
		return channel;
	}

	Dictionary getProperties() {
		return properties;
	}

	public String getURL() {
		return url;
	}

	void setProperties(Dictionary newProps) {
		properties = newProps;
	}
}
