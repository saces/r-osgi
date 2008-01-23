package ch.ethz.iks.r_osgi.service_discovery;

import ch.ethz.iks.r_osgi.URI;

public interface ServiceDiscoveryListener {

	public static final String SERVICE_INTERFACES_PROPERTY = "service.interfaces";

	public static final String FILTER_PROPERTY = "filter";

	void announceService(final String serviceInterface, final URI uri);

	void discardService(final String serviceInterface, final URI uri);

}
