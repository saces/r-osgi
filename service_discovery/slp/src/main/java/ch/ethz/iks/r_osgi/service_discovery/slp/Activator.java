package ch.ethz.iks.r_osgi.service_discovery.slp;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import ch.ethz.iks.r_osgi.service_discovery.ServiceDiscoveryHandler;

public class Activator implements BundleActivator {

	private SLPServiceDiscoveryHandler handler;

	public void start(final BundleContext context) throws Exception {
		handler = new SLPServiceDiscoveryHandler(context);
		context.registerService(ServiceDiscoveryHandler.class.getName(),
				handler, null);
	}

	public void stop(final BundleContext context) throws Exception {
		handler.shutdown();
	}

}
