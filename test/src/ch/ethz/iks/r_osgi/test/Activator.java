package ch.ethz.iks.r_osgi.test;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

import ch.ethz.iks.r_osgi.RemoteOSGiService;

public class Activator implements BundleActivator {

	private static Activator instance;

	private BundleContext context;

	private RemoteOSGiService remote;

	public void start(BundleContext context) throws Exception {
		System.out.println(ch.ethz.iks.r_osgi.sample.service.Activator.class
				.getName());
		System.out.println(ch.ethz.iks.r_osgi.transport.mina.Activator.class
				.getName());
		System.out
				.println(ch.ethz.iks.r_osgi.service_discovery.slp.Activator.class
						.getName());
		instance = this;
		this.context = context;
		final ServiceReference ref = context
				.getServiceReference(RemoteOSGiService.class.getName());
		if (ref == null) {
			throw new BundleException("R-OSGi is not present");
		}
		remote = (RemoteOSGiService) context.getService(ref);
	}

	public void stop(BundleContext context) throws Exception {
		context = null;
		remote = null;
	}

	public static Activator getActivator() {
		return instance;
	}

	public BundleContext getContext() {
		return context;
	}

	public RemoteOSGiService getR_OSGi() {
		return remote;
	}

}
