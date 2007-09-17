package ch.ethz.iks.r_osgi.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;

import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.RemoteServiceReference;

final class RemoteServiceReferenceImpl implements RemoteServiceReference {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1340731053890522970L;

	private String[] serviceInterfaces;

	private Dictionary properties;

	private URI uri;

	private transient ChannelEndpointImpl channel;

	RemoteServiceReferenceImpl(final String[] serviceInterfaces, final String fragment,
			final Dictionary properties, final ChannelEndpointImpl channel) {
		this.serviceInterfaces = serviceInterfaces;
		this.uri = channel.getRemoteEndpoint().resolve("#" + fragment);
		this.properties = properties;
		// adjust the properties
		this.properties.put(RemoteOSGiServiceImpl.SERVICE_URI, uri.toString());
		// remove the service PID, if set
		this.properties.remove("service.pid");
		// remove the R-OSGi registration property
		this.properties.remove(RemoteOSGiService.R_OSGi_REGISTRATION);
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

	public URI getURI() {
		return uri;
	}

	void setProperties(Dictionary newProps) {
		properties = newProps;
	}

	public String toString() {
		return "RemoteServiceReference{" + uri + "-"
				+ Arrays.asList(serviceInterfaces) + "}";
	}
}
