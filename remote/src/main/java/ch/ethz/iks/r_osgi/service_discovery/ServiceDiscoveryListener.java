package ch.ethz.iks.r_osgi.service_discovery;

import ch.ethz.iks.r_osgi.URI;
import java.util.Dictionary;

public interface ServiceDiscoveryListener {

	public static final String SERVICE_INTERFACES_PROPERTY = "service.interfaces";

	public static final String FILTER_PROPERTY = "filter";

	void announceService(final String serviceInterface, final URI uri,
			final Dictionary properties);

	void discardService(final String serviceInterface, final URI uri);

}
