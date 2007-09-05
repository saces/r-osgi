package ch.ethz.iks.r_osgi.service_discovery;

import java.util.Dictionary;

public interface ServiceDiscoveryListener {

	void announceService(final String serviceInterface, final String uri,
			final Dictionary properties);

	void discardService(final String serviceInterface, final String uri);

}
