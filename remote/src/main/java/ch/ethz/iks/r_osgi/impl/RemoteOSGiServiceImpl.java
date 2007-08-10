/* Copyright (c) 2006-2007 Jan S. Rellermeyer
 * Information and Communication Systems Research Group (IKS),
 * Department of Computer Science, ETH Zurich.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *    - Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    - Neither the name of ETH Zurich nor the names of its contributors may be
 *      used to endorse or promote products derived from this software without
 *      specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package ch.ethz.iks.r_osgi.impl;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import ch.ethz.iks.r_osgi.RemoteOSGiException;
import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.RemoteServiceEvent;
import ch.ethz.iks.r_osgi.RemoteServiceListener;
import ch.ethz.iks.r_osgi.RemoteServiceReference;
import ch.ethz.iks.r_osgi.SurrogateRegistration;
import ch.ethz.iks.r_osgi.Remoting;
import ch.ethz.iks.r_osgi.URL;
import ch.ethz.iks.r_osgi.channels.ChannelEndpoint;
import ch.ethz.iks.r_osgi.channels.NetworkChannelFactory;
import ch.ethz.iks.r_osgi.service_discovery.ServiceDiscoveryListener;
import ch.ethz.iks.r_osgi.types.Timestamp;
import ch.ethz.iks.util.CollectionUtils;

/**
 * <p>
 * The R-OSGi core class. Handles remote channels and subscriptions from the
 * local framework. Local services can be released for remoting and then
 * discovered by remote peers.
 * </p>
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 * @since 0.1
 */
final class RemoteOSGiServiceImpl implements RemoteOSGiService, Remoting,
		ServiceDiscoveryListener {

	/**
	 * the R-OSGi standard port.
	 */
	static int R_OSGI_PORT = 9278;

	/**
	 * the R-OSGi port property.
	 */
	static final String REMOTE_OSGi_PORT = "ch.ethz.iks.r_osgi.port";

	/**
	 * constant that holds the property string for proxy debug option.
	 */
	static final String PROXY_DEBUG_PROPERTY = "ch.ethz.iks.r_osgi.debug.proxyGeneration";

	/**
	 * constant that holds the property string for message debug option.
	 */
	static final String MSG_DEBUG_PROPERTY = "ch.ethz.iks.r_osgi.debug.messages";

	/**
	 * constant that holds the property string for internal debug option.
	 */
	static final String DEBUG_PROPERTY = "ch.ethz.iks.r_osgi.debug.internal";

	/**
	 * constant that holds the property string for SLP discovery interval time
	 * in seconds. Default is 20 seconds.
	 */
	static final String DISCOVERY_INTERVAL_PROPERTY = "ch.ethz.iks.r_osgi.remote.discoveryInterval";

	/**
	 * the event property contains the sender's url.
	 */
	static final String EVENT_SENDER_URL = "sender.url";

	/**
	 * marker for channel-registered event handlers so that they don't
	 * contribute to the peer's topic space.
	 */
	static final String R_OSGi_INTERNAL = "internal";

	/**
	 * log proxy generation debug output.
	 */
	static boolean PROXY_DEBUG;

	/**
	 * log message traffic.
	 */
	static boolean MSG_DEBUG;

	/**
	 * log method invocation debug output.
	 */
	static boolean DEBUG;

	/**
	 * the address of this peer, according to what jSLP reports.
	 */
	static String MY_ADDRESS;

	/**
	 * service reference -> remote service registration.
	 */
	private static Map serviceRegistrations = new HashMap(1);

	/**
	 * thread loop variable.
	 */
	private static boolean running = true;

	/**
	 * next transaction id.
	 */
	private static short nextXid;

	/**
	 * OSGi log service instance.
	 */
	static LogService log;

	/**
	 * the event admin tracker
	 */
	static ServiceTracker eventAdminTracker;

	/**
	 * 
	 */
	private static ServiceTracker eventHandlerTracker;

	/**
	 * 
	 */
	private static ServiceTracker remoteServiceTracker;

	/**
	 * 
	 */
	private static ServiceTracker serviceDiscoveryTracker;

	/**
	 * 
	 */
	private static ServiceTracker remoteServiceListenerTracker;

	/**
	 * the bundle context.
	 */
	static BundleContext context;

	/**
	 * the storage location.
	 */
	private final String storage;

	/**
	 * Channel ID --> ChannelEndpoint.
	 */
	private static Map channels = new HashMap(0);

	/**
	 * Channel ID --> EndpointMultiplexer
	 */
	private static Map multiplexers = new HashMap(0);

	/**
	 * creates a new RemoteOSGiServiceImpl instance.
	 * 
	 * @throws IOException
	 *             in case of IO problems.
	 */
	RemoteOSGiServiceImpl() throws IOException {
		// find out own IP address
		// TODO: allow configuration
		MY_ADDRESS = InetAddress.getAllByName(InetAddress.getLocalHost()
				.getHostName())[0].getHostAddress();

		// set the debug switches
		String prop = context.getProperty(PROXY_DEBUG_PROPERTY);
		PROXY_DEBUG = prop != null ? Boolean.valueOf(prop).booleanValue()
				: false;
		prop = context.getProperty(MSG_DEBUG_PROPERTY);
		MSG_DEBUG = prop != null ? Boolean.valueOf(prop).booleanValue() : false;
		prop = context.getProperty(DEBUG_PROPERTY);
		DEBUG = prop != null ? Boolean.valueOf(prop).booleanValue() : false;

		if (log != null) {
			if (PROXY_DEBUG) {
				log.log(LogService.LOG_INFO, "PROXY DEBUG OUTPUTS ENABLED");
			}
			if (MSG_DEBUG) {
				log.log(LogService.LOG_INFO, "MESSAGE DEBUG OUTPUTS ENABLED");
			}
			if (DEBUG) {
				log.log(LogService.LOG_INFO, "INTERNAL DEBUG OUTPUTS ENABLED");
			}
		} else {
			if (PROXY_DEBUG || MSG_DEBUG || DEBUG) {
				System.err
						.println("NO LOG SERVICE PRESENT, DEBUG PROPERTIES HAVE NO EFFECT ...");
				PROXY_DEBUG = false;
				MSG_DEBUG = false;
				DEBUG = false;
			}
		}

		// set port
		prop = context.getProperty(REMOTE_OSGi_PORT);
		R_OSGI_PORT = prop != null ? Integer.parseInt(prop) : 9278;

		// initialize the transactionID with a random value
		nextXid = (short) Math.round(Math.random() * Short.MAX_VALUE);

		// start the TCP thread
		new TCPThread().start();

		// get private storage
		final File dir = context.getDataFile("storage");
		dir.mkdirs();
		storage = dir.getAbsolutePath();

		// initialize service trackers
		eventAdminTracker = new ServiceTracker(context, EventAdmin.class
				.getName(), null);
		eventAdminTracker.open();
		if (eventAdminTracker.getTrackingCount() == 0) {
			// TODO: to log
			System.err
					.println("NO EVENT ADMIN FOUND. REMOTE EVENT DELIVERY TEMPORARILY DISABLED.");
		}

		try {
			eventHandlerTracker = new ServiceTracker(context, context
					.createFilter("(&(" + Constants.OBJECTCLASS + "="
							+ EventHandler.class.getName() + ")(!("
							+ R_OSGi_INTERNAL + "=*)))"),
					new ServiceTrackerCustomizer() {

						public Object addingService(ServiceReference reference) {
							final String[] theTopics = (String[]) reference
									.getProperty(EventConstants.EVENT_TOPIC);
							updateLeases(new LeaseUpdateMessage(theTopics, null));
							return Arrays.asList(theTopics);
						}

						public void modifiedService(ServiceReference reference,
								Object oldTopics) {

							final List oldTopicList = (List) oldTopics;
							final List newTopicList = Arrays
									.asList((String[]) reference
											.getProperty(EventConstants.EVENT_TOPIC));
							final Collection removed = CollectionUtils
									.rightDifference(newTopicList, oldTopicList);
							final Collection added = CollectionUtils
									.leftDifference(newTopicList, oldTopicList);
							final String[] addedTopics = (String[]) added
									.toArray(new String[removed.size()]);
							final String[] removedTopics = (String[]) removed
									.toArray(addedTopics);
							oldTopicList.removeAll(removed);
							oldTopicList.addAll(added);
							updateLeases(new LeaseUpdateMessage(addedTopics,
									removedTopics));
						}

						public void removedService(ServiceReference reference,
								Object oldTopics) {
							final List oldTopicsList = (List) oldTopics;
							final String[] removedTopics = (String[]) oldTopicsList
									.toArray(new String[oldTopicsList.size()]);
							updateLeases(new LeaseUpdateMessage(null,
									removedTopics));
						}
					});
			eventHandlerTracker.open();

			if (DEBUG) {
				log.log(LogService.LOG_DEBUG, "Local topic space "
						+ getTopics());
			}

			remoteServiceTracker = new ServiceTracker(context, context
					.createFilter("(" + RemoteOSGiService.R_OSGi_REGISTRATION
							+ "=*)"), new ServiceTrackerCustomizer() {

				public Object addingService(final ServiceReference reference) {
					final ServiceReference service = Arrays.asList(
							(String[]) reference
									.getProperty(Constants.OBJECTCLASS))
							.contains(SurrogateRegistration.class.getName()) ? (ServiceReference) reference
							.getProperty(SurrogateRegistration.SERVICE_REFERENCE)
							: reference;

					try {
						final RemoteServiceRegistration reg;

						final String policy = (String) reference
								.getProperty(RemoteOSGiService.R_OSGi_REGISTRATION);
						if (policy
								.equals(RemoteOSGiService.TRANSFER_BUNDLE_POLICY)) {

							// for the moment, don't accept registrations from
							// bundles that
							// have already been fetched from a remote peer.
							if (service.getBundle().getLocation().startsWith(
									"r-osgi://")) {
								return service;
							}

							reg = new BundledServiceRegistration(reference,
									service, storage);

							if (log != null) {
								log.log(LogService.LOG_INFO, "REGISTERING "
										+ reg + " WITH TRANSFER BUNDLE POLICY");
							}
						} else {
							// default: proxied service
							reg = new ProxiedServiceRegistration(reference,
									service);

							if (log != null) {
								log.log(LogService.LOG_INFO, "REGISTERING "
										+ reg + " AS PROXIED SERVICES");
							}
						}

						serviceRegistrations.put(service, reg);

						final Dictionary attribs = reg.getProperties();
						// final ServiceURL[] urls = reg.getURLs();

						// TODO: schedule for registration on the service
						// discovery layer.

						updateLeases(new LeaseUpdateMessage(
								LeaseUpdateMessage.SERVICE_ADDED, reg));
						return service;
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						throw new RemoteOSGiException("Cannot find class "
								+ service, e);
					}
				}

				public void modifiedService(ServiceReference reference,
						Object service) {
					if (reference.getProperty(R_OSGi_REGISTRATION) == null) {
						removedService(reference, service);
						return;
					}
					final RemoteServiceRegistration reg = (RemoteServiceRegistration) serviceRegistrations
							.get(reference);
					updateLeases(new LeaseUpdateMessage(
							LeaseUpdateMessage.SERVICE_MODIFIED, reg));
				}

				public void removedService(ServiceReference reference,
						Object service) {
					final RemoteServiceRegistration reg = (RemoteServiceRegistration) serviceRegistrations
							.remove(reference);
					updateLeases(new LeaseUpdateMessage(
							LeaseUpdateMessage.SERVICE_REMOVED, reg));
				}

			});
			remoteServiceTracker.open();

			remoteServiceListenerTracker = new ServiceTracker(context,
					RemoteServiceListener.class.getName(), null);
			remoteServiceListenerTracker.open();
		} catch (InvalidSyntaxException ise) {
			ise.printStackTrace();
		}

	}

	/*
	 * ------ public methods ------
	 */

	/**
	 * get the service that has been fetched under a certain
	 * <code>ServiceURL</code>.
	 * 
	 * @param url
	 *            the <code>ServiceURL</code>.
	 * @return the service object or <code>null</code> if the service is not
	 *         (yet) present.
	 * @see ch.ethz.iks.r_osgi.RemoteOSGiService#getFetchedService(ch.ethz.iks.slp.ServiceURL)
	 * @category RemoteOSGiService
	 * @since 0.6
	 */
	public Object getFetchedService(final RemoteServiceReference ref) {
		final ServiceReference sref = getFetchedServiceReference(ref);
		return ref == null ? null : context.getService(sref);
	}

	/**
	 * get the service reference for the service that has been fetched under a
	 * certain <code>ServiceURL</code>.
	 * 
	 * @param url
	 *            the <code>ServiceURL</code>.
	 * @return the service reference of the service (or service proxy) or
	 *         <code>null</code> if the service is not (yet) present.
	 * @see ch.ethz.iks.r_osgi.RemoteOSGiService#getFetchedServiceReference(ch.ethz.iks.slp.ServiceURL)
	 * @category RemoteOSGiService
	 * @since 0.6
	 */
	public ServiceReference getFetchedServiceReference(
			final RemoteServiceReference ref) {
		try {
			final ServiceReference[] refs = context.getServiceReferences(ref
					.getServiceInterfaces()[0], "(" + SERVICE_URL + "="
					+ ref.getURL() + ")");
			if (refs != null) {
				return refs[0];
			} 
		} catch (InvalidSyntaxException doesNotHappen) {
			doesNotHappen.printStackTrace();
		}
		return null;
	}

	public RemoteServiceReference[] getRemoteServiceReferences(
			final String url, final String clazz, final Filter filter)
			throws InvalidSyntaxException {
		final ChannelEndpointImpl channel = (ChannelEndpointImpl) channels
				.get(url);
		if (channel == null) {
			throw new IllegalStateException("NO CHANNEL TO " + url
					+ ", known channels " + channels);
		}
		if (clazz == null) {
			return channel.getRemoteReferences(null);
		}
		return channel.getRemoteReferences(context
				.createFilter(filter != null ? "(&(" + filter + ")("
						+ Constants.OBJECTCLASS + "=" + clazz.toString() + ")"
						: "(" + Constants.OBJECTCLASS + "=" + clazz.toString()
								+ ")"));
	}

	public RemoteServiceReference[] connect(final String url)
			throws RemoteOSGiException, UnknownHostException {
		return connect(InetAddress.getByName(URL.getHost(url)), URL
				.getPort(url), URL.getProtocol(url));
	}

	/**
	 * connect to a remote OSGi host.
	 * 
	 * @param host
	 *            the address of the remote OSGi peer.
	 * @param port
	 *            the port of the remote OSGi peer.
	 * @param protocol
	 *            the protocol to be used or <code>null</code> for default.
	 * @return the array of service urls of services offered by the remote peer.
	 * @throws RemoteOSGiException
	 *             in case of errors.
	 * @since 0.6
	 */
	public RemoteServiceReference[] connect(final InetAddress host,
			final int port, final String protocol) throws RemoteOSGiException {
		final ChannelEndpointImpl test = (ChannelEndpointImpl) channels.get(URL
				.getURL(protocol, host, port));
		if (test != null) {
			return test.getRemoteReferences(null);
		} else {
			// TODO: remove debug output
			System.out.println("REQUESTED CONNECTION TO "
					+ URL.getURL(protocol, host, port));
			System.out.println("KNOWN CHANNELS " + channels);
		}

		try {
			final ChannelEndpointImpl channel;
			if ("r-osgi".equals(protocol) || protocol == null) {
				channel = new ChannelEndpointImpl(null, host, port, protocol);
			} else {
				final ServiceReference[] refs = context.getServiceReferences(
						NetworkChannelFactory.class.getName(), "("
								+ NetworkChannelFactory.PROTOCOL_PROPERTY + "="
								+ protocol + ")");
				NetworkChannelFactory factory = (NetworkChannelFactory) context
						.getService(refs[0]);
				channel = new ChannelEndpointImpl(factory, host, port, protocol);
				if (refs == null) {
					throw new RemoteOSGiException(
							"No NetworkChannelFactory for " + protocol
									+ " found.");
				}
			}
			return channel.sendLease(getServices(), getTopics());
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new RemoteOSGiException("Connection to "
					+ URL.getURL(protocol, host, port) + " failed", ioe);
		} catch (InvalidSyntaxException e) {
			// does not happen
			e.printStackTrace();
			return null;
		}
	}

	public void disconnect(final String url) throws RemoteOSGiException {
		ChannelEndpointImpl channel = (ChannelEndpointImpl) channels.get(url);
		channel.dispose();
	}

	/**
	 * fetch the discovered remote service. The service will be fetched from the
	 * service providing host and a proxy bundle is registered with the local
	 * framework.
	 * 
	 * @param service
	 *            the <code>ServiceURL</code>.
	 * @throws RemoteOSGiException
	 *             if the fetching fails.
	 * @see ch.ethz.iks.r_osgi.RemoteOSGiService#fetchService(ch.ethz.iks.slp.ServiceURL)
	 * @since 0.1
	 * @category RemoteOSGiService
	 */
	public void fetchService(final RemoteServiceReference ref)
			throws RemoteOSGiException {
		try {
			ChannelEndpointImpl channel;
			channel = ((RemoteServiceReferenceImpl) ref).getChannel();
			channel.fetchService(ref);
		} catch (UnknownHostException e) {
			throw new RemoteOSGiException(
					"Cannot resolve host " + ref.getURL(), e);
		} catch (IOException ioe) {
			throw new RemoteOSGiException("Proxy generation error", ioe);
		} 
	}

	/**
	 * 
	 * @param sender
	 *            the sender serviceURL.
	 * @param timestamp
	 *            the Timestamp object.
	 * @return the Timestamp object that has been transformed into this peer's
	 *         local time.
	 * @throws RemoteOSGiException
	 *             if the timestamp transformation fails.
	 * @since 0.3
	 * @category RemoteOSGiService
	 */
	public Timestamp transformTimestamp(final RemoteServiceReference sender,
			final Timestamp timestamp) throws RemoteOSGiException {
		// TODO: should be the sender URL instead.

		final ChannelEndpointImpl channel = ((RemoteServiceReferenceImpl) sender)
				.getChannel();
		return channel.getOffset().transform(timestamp);
	}

	/**
	 * 
	 * @see ch.ethz.iks.r_osgi.Remoting#getEndpoint(java.lang.String)
	 * @category Remoting
	 */
	public ChannelEndpoint getEndpoint(String url) {
		System.out.println("REQUESTED ENDPOINT FOR " + url);
		System.out.println("MULTIPLEXER " + multiplexers);
		System.out.println("CHANNELS " + channels);
		EndpointMultiplexer multiplexer = (EndpointMultiplexer) multiplexers
				.get(url);
		if (multiplexer == null) {
			final String channelID = URL.getBaseURL(url);
			multiplexer = new EndpointMultiplexer((ChannelEndpoint) channels
					.get(channelID));
			multiplexers.put(url, multiplexer);
		}
		return multiplexer;
	}

	public void addRedundantEndpoint(String service, String redundant) {
		EndpointMultiplexer multiplexer = (EndpointMultiplexer) multiplexers
				.get(service);
		multiplexer.addEndpoint((ChannelEndpoint) channels.get(URL
				.getBaseURL(redundant)));
	}

	public void removeRedundantEndpoint(String service, String redundant) {
		EndpointMultiplexer multiplexer = (EndpointMultiplexer) multiplexers
				.get(service);
		multiplexer.removeEndpoint((ChannelEndpoint) channels.get(URL
				.getBaseURL(redundant)));
	}

	public void setEndpointPolicy(String service, int policy) {
		EndpointMultiplexer multiplexer = (EndpointMultiplexer) multiplexers
				.get(service);
		multiplexer.setPolicy(policy);

	}

	/**
	 * the method is called when the R-OSGi bundle is about to be stopped.
	 * removes all registered proxy bundles.
	 */
	void cleanup() {
		ChannelEndpoint[] c = (ChannelEndpoint[]) channels.values().toArray(
				new ChannelEndpoint[channels.size()]);
		channels.clear();
		for (int i = 0; i < c.length; i++) {
			c[i].dispose();
		}
	}

	/**
	 * get all provided (remote-enabled) services of this peer.
	 * 
	 * @return return the services.
	 */
	static RemoteServiceRegistration[] getServices() {
		return (RemoteServiceRegistration[]) serviceRegistrations.values()
				.toArray(
						new RemoteServiceRegistration[serviceRegistrations
								.size()]);
	}

	static RemoteServiceRegistration getService(final Long serviceID) {

		final String filter = "".equals(serviceID) ? null : '('
				+ Constants.SERVICE_ID + "=" + serviceID + ")";

		try {
			final ServiceReference[] refs = context.getServiceReferences(null,
					filter);
			if (refs == null) {
				if (log != null) {
					log.log(LogService.LOG_WARNING, "COUND NOT FIND " + filter);
				}
				return null;
			}
			return (RemoteServiceRegistration) serviceRegistrations
					.get(refs[0]);
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * get all topics for which event handlers are registered.
	 * 
	 * @return the topics.
	 */
	static String[] getTopics() {
		final Object[] topicLists = (Object[]) eventHandlerTracker
				.getServices();
		final List topics = new ArrayList();
		if (topicLists != null) {
			for (int i = 0; i < topicLists.length; i++) {
				topics.addAll((List) topicLists[i]);
			}
		}
		return (String[]) topics.toArray(new String[topics.size()]);
	}

	/**
	 * get the next transaction id.
	 * 
	 * @return the next xid.
	 */
	static synchronized short nextXid() {
		if (nextXid == -1) {
			nextXid = 0;
		}
		return (++nextXid);
	}

	/**
	 * register a channel.
	 * 
	 * @param channel
	 *            the local endpoint of the channel.
	 */
	static void registerChannel(final ChannelEndpoint channel) {
		channels.put(channel.getURL(), channel);
	}

	/**
	 * unregister a channel.
	 * 
	 * @param channel
	 *            the local endpoint of the channel.
	 */
	static void unregisterChannel(final ChannelEndpoint channel) {
		channels.remove(channel.getURL());
	}

	/**
	 * update the leases.
	 */
	static void updateLeases(final LeaseUpdateMessage msg) {
		ChannelEndpointImpl[] endpoints = (ChannelEndpointImpl[]) channels
				.values().toArray(new ChannelEndpointImpl[channels.size()]);
		for (int i = 0; i < endpoints.length; i++) {
			endpoints[i].updateLease(msg);
		}
	}

	static void notifyRemoteServiceListeners(final RemoteServiceEvent event) {
		final ServiceReference[] refs = remoteServiceListenerTracker
				.getServiceReferences();
		if (refs == null) {
			return;
		}
		final Set serviceIfaces = new HashSet(Arrays.asList(event
				.getRemoteReference().getServiceInterfaces()));
		for (int i = 0; i < refs.length; i++) {
			final String[] ifaces = (String[]) refs[i]
					.getProperty(RemoteServiceListener.SERVICE_INTERFACES);
			if (ifaces == null) {
				match(refs[i], event);
			} else {
				for (int j = 0; j < ifaces.length; j++) {
					if (serviceIfaces.contains(ifaces[j])) {
						match(refs[i], event);
						break;
					}
				}
			}
		}
	}

	private static void match(final ServiceReference ref,
			final RemoteServiceEvent event) {
		final Filter filter = (Filter) ref
				.getProperty(RemoteServiceListener.FILTER);
		if (filter == null
				|| filter.match(((RemoteServiceReferenceImpl) event
						.getRemoteReference()).getProperties())) {
			final RemoteServiceListener listener = (RemoteServiceListener) remoteServiceListenerTracker
					.getService(ref);
			listener.remoteServiceEvent(event);
		}
	}

	/*
	 * ----- private methods ------
	 */
	/*
	 * Threads
	 */

	/**
	 * TCPThread, handles incoming tcp messages.
	 */
	private final class TCPThread extends Thread {
		/**
		 * the socket.
		 */
		private final ServerSocket socket;

		/**
		 * creates and starts a new TCPThread.
		 * 
		 * @throws IOException
		 *             if the server socket cannot be opened.
		 */
		private TCPThread() throws IOException {
			socket = new ServerSocket(R_OSGI_PORT);
		}

		/**
		 * thread loop.
		 * 
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			while (running) {
				try {
					// accept incoming connections and build channel endpoints
					// for them
					new ChannelEndpointImpl(socket.accept());
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}
	}

	public void announceService(String serviceInterface, String url,
			Dictionary properties) {

	}

	public void discardService(String serviceInterface, String url) {

	}
}
