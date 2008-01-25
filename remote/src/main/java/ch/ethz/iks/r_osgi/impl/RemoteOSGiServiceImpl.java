/* Copyright (c) 2006-2008 Jan S. Rellermeyer
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

import java.io.IOException;
import java.net.InetAddress;
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
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.RemoteOSGiException;
import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.RemoteServiceEvent;
import ch.ethz.iks.r_osgi.RemoteServiceListener;
import ch.ethz.iks.r_osgi.RemoteServiceReference;
import ch.ethz.iks.r_osgi.SurrogateRegistration;
import ch.ethz.iks.r_osgi.Remoting;
import ch.ethz.iks.r_osgi.channels.ChannelEndpoint;
import ch.ethz.iks.r_osgi.channels.NetworkChannel;
import ch.ethz.iks.r_osgi.channels.NetworkChannelFactory;
import ch.ethz.iks.r_osgi.messages.LeaseUpdateMessage;
import ch.ethz.iks.r_osgi.service_discovery.ServiceDiscoveryHandler;
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
final class RemoteOSGiServiceImpl implements RemoteOSGiService, Remoting {

	/**
	 * the R-OSGi standard port.
	 */
	static int R_OSGI_PORT = 9278;

	/**
	 * the R-OSGi port property.
	 */
	static final String R_OSGi_PORT_PROPERTY = "ch.ethz.iks.r_osgi.port"; //$NON-NLS-1$

	/**
	 * register the default tcp channel? If not set to "false", the channel gets
	 * registered.
	 */
	static final String REGISTER_DEFAULT_TCP_CHANNEL = "ch.ethz.iks.r_osgi.registerDefaultChannel"; //$NON-NLS-1$

	/**
	 * constant that holds the property string for proxy debug option.
	 */
	static final String PROXY_DEBUG_PROPERTY = "ch.ethz.iks.r_osgi.debug.proxyGeneration"; //$NON-NLS-1$

	/**
	 * constant that holds the property string for message debug option.
	 */
	static final String MSG_DEBUG_PROPERTY = "ch.ethz.iks.r_osgi.debug.messages"; //$NON-NLS-1$

	/**
	 * constant that holds the property string for internal debug option.
	 */
	static final String DEBUG_PROPERTY = "ch.ethz.iks.r_osgi.debug.internal"; //$NON-NLS-1$

	/**
	 * marker for channel-registered event handlers so that they don't
	 * contribute to the peer's topic space.
	 */
	static final String R_OSGi_INTERNAL = "internal"; //$NON-NLS-1$

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
	 * the address of this peer.
	 */
	static String MY_ADDRESS;

	/**
	 * service reference -> remote service registration.
	 */
	private static Map serviceRegistrations = new HashMap(1);

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
	private static ServiceTracker remoteServiceListenerTracker;

	/**
	 * 
	 */
	private static ServiceTracker networkChannelFactoryTracker;

	/**
	 * 
	 */
	private static ServiceTracker serviceDiscoveryHandlerTracker;

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
		String prop = RemoteOSGiActivator.context
				.getProperty(PROXY_DEBUG_PROPERTY);
		PROXY_DEBUG = prop != null ? Boolean.valueOf(prop).booleanValue()
				: false;
		prop = RemoteOSGiActivator.context.getProperty(MSG_DEBUG_PROPERTY);
		MSG_DEBUG = prop != null ? Boolean.valueOf(prop).booleanValue() : false;
		prop = RemoteOSGiActivator.context.getProperty(DEBUG_PROPERTY);
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
		prop = RemoteOSGiActivator.context.getProperty(R_OSGi_PORT_PROPERTY);
		R_OSGI_PORT = prop != null ? Integer.parseInt(prop) : 9278;

		// initialize the transactionID with a random value
		nextXid = (short) Math.round(Math.random() * Short.MAX_VALUE);

		setupTrackers();
	}

	private void setupTrackers() throws IOException {

		// initialize service trackers
		eventAdminTracker = new ServiceTracker(RemoteOSGiActivator.context,
				EventAdmin.class.getName(), null);
		eventAdminTracker.open();
		if (eventAdminTracker.getTrackingCount() == 0) {
			// TODO: to log
			System.err
					.println("NO EVENT ADMIN FOUND. REMOTE EVENT DELIVERY TEMPORARILY DISABLED.");
		}

		try {
			eventHandlerTracker = new ServiceTracker(
					RemoteOSGiActivator.context, RemoteOSGiActivator.context
							.createFilter("(&(" + Constants.OBJECTCLASS + "="
									+ EventHandler.class.getName() + ")(!("
									+ R_OSGi_INTERNAL + "=*)))"),
					new ServiceTrackerCustomizer() {

						public Object addingService(ServiceReference reference) {
							final String[] theTopics = (String[]) reference
									.getProperty(EventConstants.EVENT_TOPIC);
							final LeaseUpdateMessage lu = new LeaseUpdateMessage();
							lu.setType(LeaseUpdateMessage.TOPIC_UPDATE);
							lu.setServiceID("");
							lu.setPayload(new Object[] { theTopics, null });
							updateLeases(lu);

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
							final LeaseUpdateMessage lu = new LeaseUpdateMessage();
							lu.setType(LeaseUpdateMessage.TOPIC_UPDATE);
							lu.setServiceID("");
							lu.setPayload(new Object[] { addedTopics,
									removedTopics });
							updateLeases(lu);
						}

						public void removedService(ServiceReference reference,
								Object oldTopics) {
							final List oldTopicsList = (List) oldTopics;
							final String[] removedTopics = (String[]) oldTopicsList
									.toArray(new String[oldTopicsList.size()]);
							final LeaseUpdateMessage lu = new LeaseUpdateMessage();
							lu.setType(LeaseUpdateMessage.TOPIC_UPDATE);
							lu.setServiceID("");
							lu.setPayload(new Object[] { null, removedTopics });
							updateLeases(lu);
						}
					});
			eventHandlerTracker.open();

			if (DEBUG) {
				log.log(LogService.LOG_DEBUG, "Local topic space "
						+ Arrays.asList(getTopics()));
			}

			remoteServiceListenerTracker = new ServiceTracker(
					RemoteOSGiActivator.context, RemoteServiceListener.class
							.getName(), null);
			remoteServiceListenerTracker.open();

			serviceDiscoveryHandlerTracker = new ServiceTracker(
					RemoteOSGiActivator.context, ServiceDiscoveryHandler.class
							.getName(), new ServiceTrackerCustomizer() {

						public Object addingService(ServiceReference reference) {
							// register all known services for discovery
							final ServiceDiscoveryHandler handler = (ServiceDiscoveryHandler) RemoteOSGiActivator.context
									.getService(reference);

							final RemoteServiceRegistration[] regs = (RemoteServiceRegistration[]) serviceRegistrations
									.values()
									.toArray(
											new RemoteServiceRegistration[serviceRegistrations
													.size()]);

							for (int i = 0; i < regs.length; i++) {
								handler
										.registerService(
												regs[i].getReference(),
												regs[i].getProperties(),
												URI
														.create("r-osgi://"
																+ RemoteOSGiServiceImpl.MY_ADDRESS
																+ ":"
																+ RemoteOSGiServiceImpl.R_OSGI_PORT
																+ "#"
																+ regs[i]
																		.getServiceID()));
							}
							return handler;
						}

						public void modifiedService(ServiceReference reference,
								Object service) {
							// TODO Auto-generated method stub

						}

						public void removedService(ServiceReference reference,
								Object service) {
							// TODO Auto-generated method stub

						}

					});
			serviceDiscoveryHandlerTracker.open();

			remoteServiceTracker = new ServiceTracker(
					RemoteOSGiActivator.context, RemoteOSGiActivator.context
							.createFilter("("
									+ RemoteOSGiService.R_OSGi_REGISTRATION
									+ "=*)"), new ServiceTrackerCustomizer() {

						public Object addingService(
								final ServiceReference reference) {
							// TODO: remote debug output
							System.err.println("REGISTERING NEW " + reference);
							
							// FIXME: Surrogates have to be monitored
							// separately!!!
							final ServiceReference service = Arrays
									.asList(
											(String[]) reference
													.getProperty(Constants.OBJECTCLASS))
									.contains(
											SurrogateRegistration.class
													.getName()) ? (ServiceReference) reference
									.getProperty(SurrogateRegistration.SERVICE_REFERENCE)
									: reference;

							try {
								final RemoteServiceRegistration reg = new RemoteServiceRegistration(
										reference, service);

								if (log != null) {
									log.log(LogService.LOG_INFO, "REGISTERING "
											+ reg + " AS PROXIED SERVICES");
								}

								serviceRegistrations.put(service, reg);

								registerWithServiceDiscovery(reg);

								final LeaseUpdateMessage lu = new LeaseUpdateMessage();
								lu.setType(LeaseUpdateMessage.SERVICE_ADDED);
								lu.setServiceID(String.valueOf(reg
										.getServiceID()));
								lu.setPayload(new Object[] {
										reg.getInterfaceNames(),
										reg.getProperties() });
								updateLeases(lu);
								return service;
							} catch (ClassNotFoundException e) {
								e.printStackTrace();
								throw new RemoteOSGiException(
										"Cannot find class " + service, e);
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

							registerWithServiceDiscovery(reg);

							final LeaseUpdateMessage lu = new LeaseUpdateMessage();
							lu.setType(LeaseUpdateMessage.SERVICE_MODIFIED);
							lu.setServiceID(String.valueOf(reg.getServiceID()));
							lu.setPayload(new Object[] { null,
									reg.getProperties() });
						}

						public void removedService(ServiceReference reference,
								Object service) {

							final ServiceReference sref = Arrays
									.asList(
											(String[]) reference
													.getProperty(Constants.OBJECTCLASS))
									.contains(
											SurrogateRegistration.class
													.getName()) ? (ServiceReference) reference
									.getProperty(SurrogateRegistration.SERVICE_REFERENCE)
									: reference;

							final RemoteServiceRegistration reg = (RemoteServiceRegistration) serviceRegistrations
									.remove(sref);

							// TODO: remove debug output
							System.err.println("SERVICE REMOVED " + reference
									+ " REMOTE_REG IS " + reg);

							unregisterFromServiceDiscovery(reg);

							final LeaseUpdateMessage lu = new LeaseUpdateMessage();
							lu.setType(LeaseUpdateMessage.SERVICE_REMOVED);
							lu.setServiceID(String.valueOf(reg.getServiceID()));
							lu.setPayload(new Object[] { null, null });
						}

					});
			remoteServiceTracker.open();

			networkChannelFactoryTracker = new ServiceTracker(
					RemoteOSGiActivator.context, RemoteOSGiActivator.context
							.createFilter("(" + Constants.OBJECTCLASS + "="
									+ NetworkChannelFactory.class.getName()
									+ ")"), new ServiceTrackerCustomizer() {

						public Object addingService(ServiceReference reference) {
							final NetworkChannelFactory factory = (NetworkChannelFactory) RemoteOSGiActivator.context
									.getService(reference);
							try {
								factory.activate(RemoteOSGiServiceImpl.this);
							} catch (IOException ioe) {
								// TODO: to log
								ioe.printStackTrace();
							}
							return factory;
						}

						public void modifiedService(ServiceReference reference,
								Object factory) {
						}

						public void removedService(ServiceReference reference,
								Object factory) {
						}
					});
			networkChannelFactoryTracker.open();

		} catch (InvalidSyntaxException ise) {
			ise.printStackTrace();
		}

	}

	/*
	 * ------ public methods ------
	 */

	public void registerWithServiceDiscovery(final RemoteServiceRegistration reg) {
		// register the service with all service
		// discovery
		// handler
		final Dictionary props = reg.getProperties();
		final Object[] handler = serviceDiscoveryHandlerTracker.getServices();

		if (handler != null) {
			for (int i = 0; i < handler.length; i++) {
				((ServiceDiscoveryHandler) handler[i]).registerService(reg
						.getReference(), props, URI.create("r-osgi://"
						+ RemoteOSGiServiceImpl.MY_ADDRESS + ":"
						+ RemoteOSGiServiceImpl.R_OSGI_PORT + "#"
						+ reg.getServiceID()));
			}
		}

	}

	public void unregisterFromServiceDiscovery(
			final RemoteServiceRegistration reg) {
		final Object[] handler = serviceDiscoveryHandlerTracker.getServices();
		if (handler != null) {
			for (int i = 0; i < handler.length; i++) {
				((ServiceDiscoveryHandler) handler[i])
						.unregisterService(reg.getReference());
			}
		}
	}

	/**
	 * get the service that has been fetched under a certain
	 * <code>ServiceURL</code>.
	 * 
	 * @param uri
	 *            the <code>ServiceURL</code>.
	 * @return the service object or <code>null</code> if the service is not
	 *         (yet) present.
	 * @see ch.ethz.iks.r_osgi.RemoteOSGiService#getFetchedService(ch.ethz.iks.slp.ServiceURL)
	 * @category RemoteOSGiService
	 * @since 0.6
	 */
	public Object getRemoteService(final RemoteServiceReference ref) {
		if (ref == null) {
			throw new IllegalArgumentException("Remote Reference is null.");
		}
		ServiceReference sref = getFetchedServiceReference(ref);
		if (sref == null) {
			fetchService(ref);
			sref = getFetchedServiceReference(ref);
		}
		return sref == null ? null : RemoteOSGiActivator.context
				.getService(sref);
	}

	/**
	 * get the service reference for the service that has been fetched under a
	 * certain <code>ServiceURL</code>.
	 * 
	 * @param ref
	 *            the <code>RemoteServiceReference</code> to the service.
	 * @return the service reference of the service (or service proxy) or
	 *         <code>null</code> if the service is not (yet) present.
	 * @see ch.ethz.iks.r_osgi.RemoteOSGiService#getFetchedServiceReference(ch.ethz.iks.slp.ServiceURL)
	 * @category RemoteOSGiService
	 * @since 0.6
	 */
	public ServiceReference getFetchedServiceReference(
			final RemoteServiceReference ref) {
		try {
			final ServiceReference[] refs = RemoteOSGiActivator.context
					.getServiceReferences(ref.getServiceInterfaces()[0], "("
							+ SERVICE_URI + "=" + ref.getURI() + ")");
			if (refs != null) {
				return refs[0];
			}
		} catch (InvalidSyntaxException doesNotHappen) {
			doesNotHappen.printStackTrace();
		}
		return null;
	}

	public RemoteServiceReference[] getRemoteServiceReferences(URI service,
			final String clazz, final Filter filter)
			throws InvalidSyntaxException {
		String uri = getChannelURI(service);
		ChannelEndpointImpl channel = (ChannelEndpointImpl) channels.get(uri);
		if (channel == null) {
			connect(service);
			channel = (ChannelEndpointImpl) channels.get(uri);
		}
		if (clazz == null) {
			return channel.getRemoteReferences(null);
		}
		return channel.getRemoteReferences(RemoteOSGiActivator.context
				.createFilter(filter != null ? "(&(" + filter + ")("
						+ Constants.OBJECTCLASS + "=" + clazz + ")" : "("
						+ Constants.OBJECTCLASS + "=" + clazz + ")"));
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
	public RemoteServiceReference[] connect(final URI uri)
			throws RemoteOSGiException {
		URI endpoint = URI.create(getChannelURI(uri));
		final ChannelEndpointImpl test = (ChannelEndpointImpl) channels
				.get(endpoint.toString());
		if (test != null) {
			return test.getRemoteReferences(null);
		}

		try {
			final ChannelEndpointImpl channel;
			final String protocol = endpoint.getScheme();

			final Filter filter = RemoteOSGiActivator.context.createFilter("("
					+ NetworkChannelFactory.PROTOCOL_PROPERTY + "=" + protocol
					+ ")");
			final ServiceReference[] refs = networkChannelFactoryTracker
					.getServiceReferences();
			if (refs != null) {
				for (int i = 0; i < refs.length; i++) {
					if (filter.match(refs[i])) {
						final NetworkChannelFactory factory = (NetworkChannelFactory) networkChannelFactoryTracker
								.getService(refs[i]);
						channel = new ChannelEndpointImpl(factory, endpoint);
						return channel.sendLease(getServices(), getTopics());
					}
				}
			}
			throw new RemoteOSGiException("No NetworkChannelFactory for "
					+ protocol + " found.");
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new RemoteOSGiException("Connection to " + endpoint
					+ " failed", ioe);
		} catch (InvalidSyntaxException e) {
			// does not happen
			e.printStackTrace();
			return null;
		}
	}

	public void disconnect(final URI endpoint) throws RemoteOSGiException {
		System.err.println("DISCONNECTING " + endpoint);
		ChannelEndpointImpl channel = (ChannelEndpointImpl) channels
				.get(getChannelURI(endpoint).toString());
		if (channel != null) {
			channel.dispose();
		} else {
			System.err.println("NO CHANNEL !!! " + endpoint);
		}
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
	private void fetchService(final RemoteServiceReference ref)
			throws RemoteOSGiException {
		try {
			ChannelEndpointImpl channel;
			channel = ((RemoteServiceReferenceImpl) ref).getChannel();
			channel.fetchService(ref);
		} catch (UnknownHostException e) {
			throw new RemoteOSGiException(
					"Cannot resolve host " + ref.getURI(), e);
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
	public ChannelEndpoint getEndpoint(final String uri) {
		EndpointMultiplexer multiplexer = (EndpointMultiplexer) multiplexers
				.get(uri);
		if (multiplexer == null) {
			multiplexer = new EndpointMultiplexer((ChannelEndpoint) channels
					.get(getChannelURI(URI.create(uri))));
			multiplexers.put(uri, multiplexer);
		}
		return multiplexer;
	}

	private String getChannelURI(URI serviceURI) {
		return URI.create(
				serviceURI.getScheme() + "://" + serviceURI.getHostName() + ":"
						+ serviceURI.getPort()).toString();
	}

	public void addRedundantEndpoint(URI service, URI redundant) {
		EndpointMultiplexer multiplexer = (EndpointMultiplexer) multiplexers
				.get(service.toString());
		multiplexer.addEndpoint(service, redundant, (ChannelEndpoint) channels
				.get(getChannelURI(redundant)));
	}

	public void removeRedundantEndpoint(URI service, URI redundant) {
		EndpointMultiplexer multiplexer = (EndpointMultiplexer) multiplexers
				.get(service.toString());
		multiplexer.removeEndpoint(service, redundant,
				(ChannelEndpoint) channels.get(getChannelURI(redundant)));
	}

	public void setEndpointPolicy(URI service, int policy) {
		EndpointMultiplexer multiplexer = (EndpointMultiplexer) multiplexers
				.get(service.toString());
		multiplexer.setPolicy(service, policy);

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
		final Object[] factories = networkChannelFactoryTracker.getServices();
		for (int i = 0; i < factories.length; i++) {
			try {
				((NetworkChannelFactory) factories[i]).deactivate(this);
			} catch (IOException ioe) {
				// TODO: to log
				ioe.printStackTrace();
			}
		}
		eventAdminTracker.close();
		remoteServiceTracker.close();
		serviceDiscoveryHandlerTracker.close();
		remoteServiceListenerTracker.close();
		networkChannelFactoryTracker.close();
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

	static RemoteServiceRegistration getServiceRegistration(final String serviceID) {

		final String filter = "".equals(serviceID) ? null : '('
				+ Constants.SERVICE_ID + "=" + serviceID + ")";

		try {
			final ServiceReference[] refs = RemoteOSGiActivator.context
					.getServiceReferences(null, filter);
			if (refs == null) {
				if (log != null) {
					log.log(LogService.LOG_WARNING, "COULD NOT FIND " + filter);
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
	static void registerChannelEndpoint(final ChannelEndpoint channel) {
		channels.put(channel.getRemoteAddress().toString(), channel);
	}

	/**
	 * unregister a channel.
	 * 
	 * @param channel
	 *            the local endpoint of the channel.
	 */
	static void unregisterChannel(final String channelURI) {
		channels.remove(channelURI);
	}

	/**
	 * update the leases.
	 */
	static void updateLeases(final LeaseUpdateMessage msg) {
		ChannelEndpointImpl[] endpoints = (ChannelEndpointImpl[]) channels
				.values().toArray(new ChannelEndpointImpl[channels.size()]);
		for (int i = 0; i < endpoints.length; i++) {
			endpoints[i].sendLeaseUpdate(msg);
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
			if (listener != null) {
				listener.remoteServiceEvent(event);
			}
		}
	}

	public void createEndpoint(final NetworkChannel channel) {
		new ChannelEndpointImpl(channel);
	}

	public void ungetRemoteService(RemoteServiceReference remoteServiceReference) {
		((RemoteServiceReferenceImpl) remoteServiceReference).getChannel()
				.ungetRemoteService(remoteServiceReference.getURI());

	}

	public URI getListeningAddress(final String protocol) {
		try {
			final ServiceReference[] refs = RemoteOSGiActivator.context
					.getServiceReferences(
							NetworkChannelFactory.class.getName(), "("
									+ NetworkChannelFactory.PROTOCOL_PROPERTY
									+ "=" + protocol + ")");
			return refs == null ? null
					: ((NetworkChannelFactory) RemoteOSGiActivator.context
							.getService(refs[0])).getListeningAddress(protocol);
		} catch (InvalidSyntaxException ise) {
			ise.printStackTrace();
			return null;
		}
	}

}
