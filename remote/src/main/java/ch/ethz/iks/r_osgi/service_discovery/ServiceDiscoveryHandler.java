package ch.ethz.iks.r_osgi.service_discovery;

public interface ServiceDiscoveryHandler {

	void registerForDiscovery(ServiceDiscoveryListener listener,
			final String serviceInterface, final String filter);

	void unregisterForDiscovery(ServiceDiscoveryListener listener,
			final String serviceInterface, final String filter);

}
