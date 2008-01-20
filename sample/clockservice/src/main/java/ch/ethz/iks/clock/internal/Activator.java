package ch.ethz.iks.clock.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;

import ch.ethz.iks.clock.Clock;
import ch.ethz.iks.r_osgi.RemoteOSGiService;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws BundleException {
			final ServiceReference sref = context
					.getServiceReference(EventAdmin.class.getName());
			if (sref == null) {
				throw new BundleException("No EventAdmin present.");
			}
			Dictionary properties = new Hashtable();
			properties.put(RemoteOSGiService.R_OSGi_REGISTRATION,
					RemoteOSGiService.SERVICE_PROXY_POLICY);
			properties.put(RemoteOSGiService.PRESENTATION, "ch.ethz.iks.clock.internal.ClockUI");
			context.registerService(Clock.class.getName(), new ClockImpl(
					(EventAdmin) context.getService(sref)), properties);
		}

	public void stop(BundleContext context) throws Exception {

	}

}
