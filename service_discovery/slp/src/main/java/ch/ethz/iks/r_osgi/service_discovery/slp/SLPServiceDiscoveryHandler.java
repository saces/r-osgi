package ch.ethz.iks.r_osgi.service_discovery.slp;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.RemoteOSGiException;
import ch.ethz.iks.r_osgi.service_discovery.ServiceDiscoveryHandler;
import ch.ethz.iks.r_osgi.service_discovery.ServiceDiscoveryListener;
import ch.ethz.iks.slp.Advertiser;
import ch.ethz.iks.slp.Locator;
import ch.ethz.iks.slp.ServiceLocationEnumeration;
import ch.ethz.iks.slp.ServiceLocationException;
import ch.ethz.iks.slp.ServiceType;
import ch.ethz.iks.slp.ServiceURL;
import ch.ethz.iks.util.ScheduleListener;
import ch.ethz.iks.util.Scheduler;

public class SLPServiceDiscoveryHandler implements ServiceDiscoveryHandler,
		ScheduleListener {

	private static final int DISCOVERY_INTERVAL = 10;

	/**
	 * SLP registration scheduler.
	 */
	private final Scheduler reregistration = new Scheduler(this);

	private Advertiser advertiser;

	private Locator locator;

	private Map registrations = new HashMap();

	private ServiceTracker discoveryListenerTracker;

	private boolean hasListeners = false;

	private ArrayList knownServices = new ArrayList();

	private ArrayList warningList = new ArrayList();

	private DiscoveryThread thread;

	private static final ServiceType OSGI = new ServiceType("service:osgi");

	/**
	 * constant that holds the property string for SLP registration lifetime.
	 * Default is 60 seconds.
	 */
	private static final String DEFAULT_SLP_LIFETIME_PROPERTY = "ch.ethz.iks.r_osgi.remote.defaultLifetime";

	/**
	 * default lifetime for SLP registration.
	 */
	static int DEFAULT_SLP_LIFETIME;

	public SLPServiceDiscoveryHandler(final BundleContext context) {
		final String prop = context.getProperty(DEFAULT_SLP_LIFETIME_PROPERTY);
		DEFAULT_SLP_LIFETIME = prop != null ? Integer.parseInt(prop) : 90;

		final ServiceReference advRef = context
				.getServiceReference(Advertiser.class.getName());
		final ServiceReference locRef = context
				.getServiceReference(Locator.class.getName());
		if (advRef == null || locRef == null) {
			throw new RuntimeException("jSLP is not running.");
		}
		this.advertiser = (Advertiser) context.getService(advRef);
		this.locator = (Locator) context.getService(locRef);

		if (DISCOVERY_INTERVAL > 0) {
			thread = new DiscoveryThread();
			thread.start();

			try {
				discoveryListenerTracker = new ServiceTracker(context, context
						.createFilter("(" + Constants.OBJECTCLASS + "="
								+ ServiceDiscoveryListener.class.getName()
								+ ")"), new ServiceTrackerCustomizer() {

					public Object addingService(ServiceReference reference) {
						synchronized (thread) {
							if (!hasListeners) {
								hasListeners = true;
								thread.notifyAll();
							}
						}

						// TODO: modify the query

						return context.getService(reference);
					}

					public void modifiedService(ServiceReference reference,
							Object service) {
						// TODO: modify the query

					}

					public void removedService(ServiceReference reference,
							Object service) {
						// TODO: modify the query

						if (discoveryListenerTracker.getTrackingCount() == 0) {
							synchronized (thread) {
								hasListeners = true;
								thread.notifyAll();
							}
						}
					}
				});
				discoveryListenerTracker.open();
			} catch (InvalidSyntaxException ise) {
				// should not happen
				ise.printStackTrace();
			}
		}
	}

	public void registerService(final ServiceReference ref,
			final Dictionary properties, final URI uri) {
		try {
			final SLPServiceRegistration reg = new SLPServiceRegistration(ref,
					properties, uri);
			registrations.put(ref, reg);
			reg.register(advertiser);
			reregistration.schedule(reg, System.currentTimeMillis()
					+ (SLPServiceDiscoveryHandler.DEFAULT_SLP_LIFETIME - 1)
					* 1000);
		} catch (ServiceLocationException slp) {
			slp.printStackTrace();
		}
	}

	public void unregisterService(final ServiceReference ref) {
		final SLPServiceRegistration reg = (SLPServiceRegistration) registrations
				.get(ref);
		try {
			reg.unregister(advertiser);
		} catch (ServiceLocationException slp) {
			slp.printStackTrace();
		}
		reregistration.unschedule(reg);
	}

	public void due(Scheduler scheduler, long timestamp, Object object) {
		final SLPServiceRegistration reg = (SLPServiceRegistration) object;
		try {
			reg.register(advertiser);
		} catch (ServiceLocationException slp) {
			slp.printStackTrace();
		}
		scheduler.reschedule(reg, System.currentTimeMillis()
				+ (SLPServiceDiscoveryHandler.DEFAULT_SLP_LIFETIME - 1) * 1000);
	}

	private void announceService(ServiceURL service) {
		final ServiceReference[] refs = discoveryListenerTracker
				.getServiceReferences();
		final String serviceInterfaceName = service.getServiceType()
				.getConcreteTypeName().replace('/', '.');
		final ArrayList hitList = new ArrayList();
		for (int i = 0; i < refs.length; i++) {
			final String[] interfaces = (String[]) refs[i]
					.getProperty(ServiceDiscoveryListener.SERVICE_INTERFACES_PROPERTY);
			if (interfaces == null) {
				hitList.add(refs[i]);
				break;
			}
			for (int j = 0; j < interfaces.length; j++) {
				if (interfaces[j].equals(serviceInterfaceName)) {
					hitList.add(refs[i]);
					break;
				}
			}
		}

		if (hitList.isEmpty()) {
			return;
		}

		// now, we have the references which have requested this service
		// next, get the service properties and check the filters
		try {
			ServiceLocationEnumeration sle = locator.findAttributes(service,
					null, null);
			Dictionary properties = new Hashtable();
			while (sle.hasMoreElements()) {
				String a = (String) sle.next();
				System.out.println("ATTRIBUTE: " + a);
			}

			// TODO: check the filter...
			final URI uri = URI.create(service.getProtocol() + "://"
					+ service.getHost() + ":" + service.getPort() + "#"
					+ service.getURLPath().substring(1));
			final ServiceReference[] hits = (ServiceReference[]) hitList
					.toArray(new ServiceReference[hitList.size()]);
			for (int i = 0; i < hits.length; i++) {
				final Filter filter = (Filter) hits[i]
						.getProperty(ServiceDiscoveryListener.FILTER_PROPERTY);
				if (filter == null || filter.match(properties)) {
					((ServiceDiscoveryListener) discoveryListenerTracker
							.getService(hits[i])).announceService(
							serviceInterfaceName, uri, properties);
				}
			}
		} catch (ServiceLocationException slp) {
			slp.printStackTrace();
		}
	}

	private void discardService(ServiceURL lostService) {
		// TODO Auto-generated method stub

	}

	private final class DiscoveryThread extends Thread {

		public void run() {
			try {
				while (!isInterrupted()) {
					// in case nobody listens, don't do any discovery
					synchronized (this) {
						if (!hasListeners) {
							wait();
						}
					}

					try {
						// initially contains all known services
						final List lostServices = new ArrayList(knownServices);

						// find all services of type osgi
						final ServiceLocationEnumeration services = locator
								.findServices(OSGI, null, null);

						while (services.hasMoreElements()) {
							final ServiceURL service = (ServiceURL) services
									.next();

							// FIXME: this is not true anymore !!!
							// if (service.getHost().equals(MY_ADDRESS)) {
							// continue;
							// }
							if (!knownServices.contains(service)) {
								announceService(service);
								knownServices.add(service);
							}
							// seen, so remove from lost list
							lostServices.remove(service);

						}

						// notify the listeners for all lost services
						for (Iterator iter = lostServices.iterator(); iter
								.hasNext();) {
							ServiceURL lostService = (ServiceURL) iter.next();
							if (!warningList.contains(lostService)) {
								warningList.add(lostService);
							} else {
								warningList.remove(lostService);
								knownServices.remove(lostService);
								// be polite: first notify the listeners and
								// then unregister the proxy bundle ...
								discardService(lostService);
							}
						}
					} catch (ServiceLocationException sle) {
						sle.printStackTrace();
					} catch (RemoteOSGiException re) {
						re.printStackTrace();
					}
					Thread.sleep(DISCOVERY_INTERVAL);
				}
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
	}
}
