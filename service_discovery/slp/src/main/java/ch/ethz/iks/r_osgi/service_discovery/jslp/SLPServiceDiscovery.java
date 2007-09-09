package ch.ethz.iks.r_osgi.service_discovery.jslp;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import ch.ethz.iks.r_osgi.RemoteOSGiException;
import ch.ethz.iks.r_osgi.RemoteServiceListener;
import ch.ethz.iks.r_osgi.channels.ChannelEndpoint;
import ch.ethz.iks.r_osgi.service_discovery.ServiceDiscoveryHandler;
import ch.ethz.iks.r_osgi.service_discovery.ServiceDiscoveryListener;
import ch.ethz.iks.util.ScheduleListener;
import ch.ethz.iks.util.Scheduler;

public class SLPServiceDiscovery implements ServiceDiscoveryHandler, ScheduleListener {

	private static final int DISCOVERY_INTERVAL = 10;

	/**
	 * SLP reregistration scheduler.
	 */
	private final Scheduler reregistration = new Scheduler(this);
	
	/**
	 * constant that holds the property string for SLP registration lifetime.
	 * Default is 60 seconds.
	 */
	private static final String DEFAULT_SLP_LIFETIME_PROPERTY = "ch.ethz.iks.r_osgi.remote.defaultLifetime";

	/**
	 * default lifetime for SLP registration.
	 */
	private static int DEFAULT_SLP_LIFETIME;
	
	
	public SLPServiceDiscovery() {
		if (DISCOVERY_INTERVAL > 0) {
			new DiscoveryThread().start();
		}
		
		// prop = context.getProperty(DEFAULT_SLP_LIFETIME_PROPERTY);
		// DEFAULT_SLP_LIFETIME = prop != null ? Integer.parseInt(prop) : 90;
	}
	/*
	public SLPServiceDiscovery() {
		// start the discovery thread
		if (DISCOVERY_INTERVAL > 0) {
			new DiscoveryThread().start();
		}
		prop = context.getProperty(DEFAULT_SLP_LIFETIME_PROPERTY);
		DEFAULT_SLP_LIFETIME = prop != null ? Integer.parseInt(prop) : 90;

	}
	
	public void registerForDiscovery() {
		// TODO Auto-generated method stub
		
	}

	public void unregisterForDiscovery() {
		// TODO Auto-generated method stub
		
	}
	
	public void registerService() {
		
		"service:osgi:"
		+ interfaceNames[i].replace('.', '/') + "://"
		+ RemoteOSGiServiceImpl.MY_ADDRESS + ":"
		+ RemoteOSGiServiceImpl.R_OSGI_PORT + "/" + serviceID,
		RemoteOSGiServiceImpl.DEFAULT_SLP_LIFETIME
		reregistration.schedule(reg, System.currentTimeMillis()
				+ (DEFAULT_SLP_LIFETIME - 1) * 1000);

		for (int i = 0; i < urls.length; i++) {
			advertiser.register(urls[i], attribs);
		}

	}
	
	private void notifyDiscovery(final ServiceURL service)
			throws RemoteOSGiException {
		if (DEBUG) {
			log.log(LogService.LOG_DEBUG, "discovered " + service);
		}

		final String interfaceName = service.getServiceType()
				.getConcreteTypeName().replace('/', '.');
		try {
			final ServiceReference[] refs = context.getServiceReferences(
					RemoteServiceListener.class.getName(), "(|("
							+ RemoteServiceListener.SERVICE_INTERFACES + "="
							+ interfaceName + ")(!("
							+ RemoteServiceListener.SERVICE_INTERFACES + "=*)))");
			if (refs != null) {
				for (int i = 0; i < refs.length; i++) {
					((RemoteServiceListener) context.getService(refs[i]))
							.notifyDiscovery(service);
					if (refs[i].getProperty(RemoteServiceListener.AUTO_FETCH) != null) {
						fetchService(service);
					}
				}
			}

		} catch (InvalidSyntaxException i) {
			i.printStackTrace();
		}

		if (log != null) {
			log.log(LogService.LOG_DEBUG, "DISCOVERED " + service);
		}
	}
	
	private void notifyServiceLost(final ServiceURL service) {
		final String interfaceName = service.getServiceType()
				.getConcreteTypeName().replace('/', '.');
		try {
			final ServiceReference[] refs = context.getServiceReferences(
					RemoteServiceListener.class.getName(), "("
							+ RemoteServiceListener.SERVICE_INTERFACES + "="
							+ interfaceName + ")");

			if (refs != null) {
				for (int i = 0; i < refs.length; i++) {
					((RemoteServiceListener) context.getService(refs[i]))
							.notifyServiceLost(service);
				}
			}
		} catch (InvalidSyntaxException i) {
			i.printStackTrace();
		}
	}
	
	
	public void due(final Scheduler scheduler, final long timestamp,
			final Object object) {
		final RemoteServiceRegistration reg = (RemoteServiceRegistration) object;

		try {
			final ServiceURL[] urls = reg.getURLs();
			final Dictionary properties = reg.getProperties();

			for (int i = 0; i < urls.length; i++) {
				advertiser.register(urls[i], properties);
			}
			final long next = System.currentTimeMillis()
					+ ((DEFAULT_SLP_LIFETIME - 1) * 1000);
			scheduler.reschedule(reg, next);
		} catch (ServiceLocationException sle) {
			sle.printStackTrace();
		}
	}

	*/

	public void registerForDiscovery(ServiceDiscoveryListener listener, String serviceInterface, String filter) {
		// TODO Auto-generated method stub
		
	}

	public void unregisterForDiscovery(ServiceDiscoveryListener listener, String serviceInterface, String filter) {
		// TODO Auto-generated method stub
		
	}

	public void due(Scheduler scheduler, long timestamp, Object object) {
		// TODO Auto-generated method stub
		
	}

	private final class DiscoveryThread extends Thread {

		public void run() {
			try {
				while (running) {
					// in case nobody listens, don't do any discovery
					synchronized (hasListeners) {
						if (hasListeners == Boolean.FALSE) {
							hasListeners.wait();
						}
					}

					try {
						// initially contains all known services
						final List lostServices = new ArrayList(knownServices);

						// find all services of type osgi
						try {
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
									notifyDiscovery(service);
									knownServices.add(service);
								}
								// seen, so remove from lost list
								lostServices.remove(service);

							}
						} catch (InvalidSyntaxException ise) {
							// does not happen
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
								notifyServiceLost(lostService);

								// dispose channel
								ChannelEndpoint c = (ChannelEndpoint) channels
										.get(lostService);
								if (c != null) {
									c.dispose();
								}
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
