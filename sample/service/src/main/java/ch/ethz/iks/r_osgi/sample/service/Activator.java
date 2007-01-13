package ch.ethz.iks.r_osgi.sample.service;

import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.sample.api.ServiceInterface;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {

		// register the sample service and enable R-OSGi remote access by
		// building a proxy on the client side
		Hashtable properties = new Hashtable();
		properties.put(RemoteOSGiService.R_OSGi_REGISTRATION,
				RemoteOSGiService.USE_PROXY_POLICY);
		properties.put(RemoteOSGiService.SMART_PROXY, SmartService.class
				.getName());

		// properties.put(RemoteOSGiService.R_OSGi_REGISTRATION,
		// RemoteOSGiService.TRANSFER_BUNDLE_POLICY);

		context.registerService(ServiceInterface.class.getName(),
				new ServiceImpl(), properties);

		System.out.println("Registered service "
				+ ServiceInterface.class.getName());

		final ServiceReference ref = context
				.getServiceReference(EventAdmin.class.getName());
		if (ref != null) {
			final EventAdmin eventAdmin = (EventAdmin) context.getService(ref);
			new Thread() {
				public void run() {
					final Event event = new Event("test/topic", null);
					while (true) {
						System.out.println();
						System.out.println("SENDING EVENT " + event);
						eventAdmin.postEvent(event);
						try {
							Thread.sleep(30000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}.start();
		} else {
			System.err.println();
			System.err
					.println("NO EVENT ADMIN FOUND. CANNOT SEND REMOTE EVENTS.");
			System.err.println();
		}
	}

	public void stop(BundleContext context) throws Exception {

	}

}
