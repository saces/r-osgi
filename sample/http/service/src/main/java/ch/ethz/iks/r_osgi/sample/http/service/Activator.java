package ch.ethz.iks.r_osgi.sample.http.service;

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
		Hashtable properties = new Hashtable();
		properties.put(RemoteOSGiService.R_OSGi_REGISTRATION,
				RemoteOSGiService.USE_PROXY_POLICY);
		context.registerService(ServiceInterface.class.getName(),
				new TestServiceImpl(), properties);
		ServiceReference ref = context.getServiceReference(EventAdmin.class
				.getName());
		if (ref == null) {
			System.err
					.println("NO EVENT ADMIN FOUND; EVENT DELIVERY IS DISABLED");
		} else {
			new EventGenerator((EventAdmin) context.getService(ref)).start();
		}
	}

	public void stop(BundleContext context) throws Exception {

	}

	private class EventGenerator extends Thread {
		private EventAdmin eadmin;

		private Event event;

		private EventGenerator(EventAdmin ea) {
			eadmin = ea;
			event = new Event("test/event", null);
		}

		public void run() {
			try {
				while (!Thread.interrupted()) {
					System.out.println("XXXXXXXXXX POSTING EVENT " + event);
					eadmin.postEvent(event);
					Thread.sleep(2000L);
				}
			} catch (InterruptedException ie) {

			}
		}
	}

}
