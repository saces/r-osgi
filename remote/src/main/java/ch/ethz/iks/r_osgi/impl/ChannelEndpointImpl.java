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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.RemoteOSGiException;
import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.RemoteServiceEvent;
import ch.ethz.iks.r_osgi.RemoteServiceReference;
import ch.ethz.iks.r_osgi.channels.ChannelEndpoint;
import ch.ethz.iks.r_osgi.channels.NetworkChannel;
import ch.ethz.iks.r_osgi.channels.NetworkChannelFactory;
import ch.ethz.iks.r_osgi.messages.RemoteOSGiMessage;
import ch.ethz.iks.r_osgi.messages.DeliverBundleMessage;
import ch.ethz.iks.r_osgi.messages.DeliverServiceMessage;
import ch.ethz.iks.r_osgi.messages.FetchServiceMessage;
import ch.ethz.iks.r_osgi.messages.InvokeMethodMessage;
import ch.ethz.iks.r_osgi.messages.LeaseMessage;
import ch.ethz.iks.r_osgi.messages.LeaseUpdateMessage;
import ch.ethz.iks.r_osgi.messages.MethodResultMessage;
import ch.ethz.iks.r_osgi.messages.RemoteEventMessage;
import ch.ethz.iks.r_osgi.messages.TimeOffsetMessage;

/**
 * <p>
 * The endpoint of a network channel encapsulates most of the communication
 * logic like sending of messages, service method invocation, timestamp
 * synchronization, and event delivery.
 * </p>
 * <p>
 * Endpoints exchange symmetric leases when they are established. These leases
 * contain the statements of supply and demand. The peer states the services it
 * offers and the event topics it is interested in. Whenever one of these
 * statements undergo a change, a lease update has to be sent. Leases expire
 * with the closing of the network channel and the two endpoints.
 * </p>
 * <p>
 * The network transport of channels is modular and exchangeable. Services can
 * state the supported protocols in their service url. R-OSGi maintains a list
 * of network channel factories and the protocols they support. Each channel
 * uses exactly one protocol.
 * <p>
 * <p>
 * When the network channel breaks down, the channel endpoint tries to reconnect
 * and to restore the connection. If this is not possible (for instance, because
 * the other endpoint is not available any more, the endpoint is unregistered.
 * </p>
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 */
public final class ChannelEndpointImpl implements ChannelEndpoint {

	/**
	 * the channel.
	 */
	private NetworkChannel networkChannel;

	/**
	 * the services provided by the OSGi framework holding the remote channel
	 * endpoint. Map of service URI -> RemoteServiceReferences
	 */
	private Map remoteServices = new HashMap(0);

	/**
	 * the topics of interest of the OSGi framework holding the remote channel
	 * endpoint. List of topic strings
	 */
	private List remoteTopics = new ArrayList(0);

	/**
	 * the time offset between this peer's local time and the local time of the
	 * remote channel endpoint.
	 */
	private TimeOffset timeOffset;

	/**
	 * Timeout.
	 */
	private static final int TIMEOUT = 120000;

	/**
	 * the receiver queue.
	 */
	private final Map receiveQueue = new HashMap(0);

	/**
	 * map of service url -> RemoteServiceRegistration.
	 */
	private final HashMap localServices = new HashMap(2);

	/**
	 * map of service url -> service registration.
	 */
	private final HashMap proxiedServices = new HashMap(0);

	/**
	 * map of service url -> proxy bundle. If the endpoint is closed, the
	 * proxies are unregistered.
	 */
	private final HashMap proxyBundles = new HashMap(0);

	/**
	 * map of service url -> proxy class
	 */
	private final HashMap proxies = new HashMap(0);

	/**
	 * the handler registration, if the remote topic space is not empty.
	 */
	private ServiceRegistration handlerReg = null;

	/**
	 * keeps track if the channel endpoint has lost its connection to the other
	 * endpoint.
	 */
	private boolean reconnecting = false;

	/**
	 * dummy object used for blocking method calls until the result message has
	 * arrived.
	 */
	private static final Object WAITING = new Object();

	/**
	 * filter for events to prevent loops in the remote delivery if the peers
	 * connected by this channel have non-disjoint topic spaces.
	 */
	private static final String NO_LOOPS = "(!("
			+ RemoteEventMessage.EVENT_SENDER_URI + "=*))";

	/**
	 * create a new channel endpoint.
	 * 
	 * @param factory
	 *            the transport channel factory.
	 * @param address
	 *            the remote endpoint address.
	 * @param port
	 *            the remote endpoint port.
	 * @param protocol
	 *            the protocol of the channel.
	 * @throws RemoteOSGiException
	 *             if something goes wrong in R-OSGi.
	 * @throws IOException
	 *             if something goes wrong on the network layer.
	 */
	ChannelEndpointImpl(final NetworkChannelFactory factory,
			final URI endpointURI) throws RemoteOSGiException, IOException {
		this.networkChannel = factory.getConnection(this, endpointURI);
		if (RemoteOSGiServiceImpl.DEBUG) {
			RemoteOSGiServiceImpl.log.log(LogService.LOG_DEBUG,
					"opening new channel " + getRemoteEndpoint());
		}
		RemoteOSGiServiceImpl.registerChannelEndpoint(this);
	}

	/**
	 * create a new channel endpoint from an incoming connection.
	 * 
	 * @param socket
	 *            the socket on which the incoming connection was accepted.
	 * @throws IOException
	 *             if something goes wrong on the network layer.
	 */
	ChannelEndpointImpl(final NetworkChannel channel) {
		this.networkChannel = channel;
		channel.bind(this);
		RemoteOSGiServiceImpl.registerChannelEndpoint(this);
	}

	RemoteServiceReference[] sendLease(
			final RemoteServiceRegistration[] myServices,
			final String[] myTopics) {
		final LeaseMessage l = new LeaseMessage();
		populateLease(l, myServices, myTopics);
		final LeaseMessage lease = (LeaseMessage) sendMessage(l);
		return processLease(lease);
	}

	void updateLease(final LeaseUpdateMessage msg) {
		send(msg);
	}

	/**
	 * get the channel ID.
	 * 
	 * @return the channel ID.
	 * @see ch.ethz.iks.r_osgi.channels.ChannelEndpoint#getURL() *
	 * @category ChannelEndpoint
	 */
	public URI getRemoteEndpoint() {
		return networkChannel.getRemoteEndpoint();
	}

	URI getLocalEndpoint() {
		return networkChannel.getLocalEndpoint();
	}

	/**
	 * process a recieved message. Called by the channel.
	 * 
	 * @param msg
	 *            the received message.
	 * @see ch.ethz.iks.r_osgi.channels.ChannelEndpoint#receivedMessage(ch.ethz.iks.r_osgi.RemoteOSGiMessage)
	 * @category ChannelEndpoint
	 */
	public void receivedMessage(final RemoteOSGiMessage msg) {
		if (msg == null) {
			if (RemoteOSGiServiceImpl.DEBUG) {
				RemoteOSGiServiceImpl.log.log(LogService.LOG_WARNING,
						"Connection to " + getRemoteEndpoint()
								+ " broke down. Trying to reconnect ...");
			}
			try {
				if (!reconnecting) {
					reconnecting = true;
					networkChannel.reconnect();
					reconnecting = false;
				}
			} catch (IOException ioe) {
				dispose();
				return;
			}
		}
		final Integer xid = new Integer(msg.getXID());
		synchronized (receiveQueue) {
			final Object state = receiveQueue.get(xid);
			if (state == WAITING) {
				receiveQueue.put(xid, msg);
				receiveQueue.notifyAll();
				return;
			} else {
				final RemoteOSGiMessage reply = handleMessage(msg);
				if (reply != null) {
					try {
						networkChannel.sendMessage(reply);
					} catch (IOException e) {
						// TODO: remove debug output
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * invoke a method on the remote host. This function is used by all proxy
	 * bundles.
	 * 
	 * @param service
	 *            the service URL.
	 * @param methodSignature
	 *            the method signature.
	 * @param args
	 *            the method parameter.
	 * @return the result of the remote method invokation.
	 * @see ch.ethz.iks.r_osgi.Remoting#invokeMethod(java.lang.String,
	 *      java.lang.String, java.lang.String, java.lang.Object[])
	 * @category ChannelEndpoint
	 */
	public Object invokeMethod(final String service,
			final String methodSignature, final Object[] args) throws Throwable {
		if (networkChannel == null) {
			throw new RemoteOSGiException("Network channel went down");
		}
		final InvokeMethodMessage invokeMsg = new InvokeMethodMessage();
		invokeMsg.setServiceID(URI.create(service).getFragment());
		invokeMsg.setMethodSignature(methodSignature);
		invokeMsg.setArgs(args);

		try {
			// send the message and get a MethodResultMessage in return
			final MethodResultMessage result = (MethodResultMessage) sendMessage(invokeMsg);
			if (result.causedException()) {
				throw result.getException();
			}
			return result.getResult();
		} catch (RemoteOSGiException e) {
			throw new RemoteOSGiException("Method invocation of "
					+ methodSignature + " failed.", e);
		}
	}

	/**
	 * dispose the channel.
	 * 
	 * @category ChannelEndpoint
	 */
	public void dispose() {
		if (RemoteOSGiServiceImpl.DEBUG) {
			RemoteOSGiServiceImpl.log.log(LogService.LOG_DEBUG,
					"DISPOSING ENDPOINT " + getRemoteEndpoint());
		}
		RemoteOSGiServiceImpl.unregisterChannel(this);
		if (handlerReg != null) {
			handlerReg.unregister();
		}
		Bundle[] bundles = (Bundle[]) proxyBundles.values().toArray(
				new Bundle[proxyBundles.size()]);
		for (int i = 0; i < bundles.length; i++) {
			try {
				if (bundles[i].getState() != Bundle.UNINSTALLED) {
					// bundles[i].uninstall();
				}
			} catch (Throwable t) {
				// don't care
			}
		}
		networkChannel = null;
		remoteServices = null;
		remoteTopics = null;
		timeOffset = null;
		receiveQueue.clear();
		localServices.clear();
		proxiedServices.clear();
		proxyBundles.clear();
		handlerReg = null;
		reconnecting = true;
		synchronized (receiveQueue) {
			receiveQueue.notifyAll();
		}
	}

	/**
	 * get the attributes of a service. This function is used to simplify proxy
	 * bundle generation.
	 * 
	 * @param serviceURL
	 *            the serviceURL of the remote service.
	 * @return the service attributes.
	 * @category ChannelEndpoint
	 */
	public Dictionary getProperties(final String service) {
		return getRemoteReference(service).getProperties();
	}

	private RemoteServiceReferenceImpl getRemoteReference(final String uri) {
		return (RemoteServiceReferenceImpl) remoteServices.get(uri);
	}

	/**
	 * 
	 * @param filter
	 * @return
	 */
	RemoteServiceReference[] getRemoteReferences(final Filter filter) {
		final List result = new ArrayList();
		final RemoteServiceReferenceImpl[] refs = (RemoteServiceReferenceImpl[]) remoteServices
				.values().toArray(
						new RemoteServiceReferenceImpl[remoteServices.size()]);
		if (filter == null) {
			return refs;
		} else {
			for (int i = 0; i < refs.length; i++) {
				if (filter.match(refs[i].getProperties())) {
					result.add(refs[i]);
				}
			}
			return (RemoteServiceReference[]) result
					.toArray(new RemoteServiceReferenceImpl[result.size()]);
		}
	}

	/**
	 * get the attributes for the presentation of the service. This function is
	 * used by proxies that support ServiceUI presentations.
	 * 
	 * @param serviceURL
	 *            the serviceURL of the remote service.
	 * @return the presentation attributes.
	 * @category ChannelEndpoint
	 */
	public Dictionary getPresentationProperties(final String service) {
		final Dictionary attribs = new Hashtable();
		attribs.put(RemoteOSGiServiceImpl.SERVICE_URI, service);
		attribs.put(RemoteOSGiService.PRESENTATION, getRemoteReference(service)
				.getProperty(RemoteOSGiService.PRESENTATION));
		return attribs;
	}

	/**
	 * 
	 * @param service
	 * @param reg
	 * @category ChannelEndpoint
	 */
	public void trackRegistration(final String service,
			final ServiceRegistration reg) {
		proxiedServices.put(service, reg);
	}

	/**
	 * 
	 * @param service
	 * @category ChannelEndpoint
	 */
	public void untrackRegistration(final String service) {
		proxiedServices.remove(service);
	}

	/**
	 * fetch the service from the remote peer.
	 * 
	 * @param service
	 *            the service url.
	 * @throws IOException
	 *             in case of network errors.
	 * @throws BundleException
	 *             if the installation of the proxy or the migrated bundle
	 *             fails.
	 */
	void fetchService(final RemoteServiceReference ref) throws IOException,
			RemoteOSGiException {

		// build the FetchServiceMessage
		final FetchServiceMessage fetchReq = new FetchServiceMessage();
		fetchReq.setServiceID(ref.getURI().getFragment());

		// send the FetchServiceMessage and get a DeliverServiceMessage in
		// return
		final RemoteOSGiMessage msg = sendMessage(fetchReq);
		String bundleLocation = null;
		try {
			if (msg instanceof DeliverServiceMessage) {
				final DeliverServiceMessage deliv = (DeliverServiceMessage) msg;
				final URI service = networkChannel.getRemoteEndpoint().resolve(
						"#" + deliv.getServiceID());

				// generate a proxy bundle for the service
				bundleLocation = new ProxyGenerator().generateProxyBundle(
						service, deliv);

				// install the proxy bundle
				final Bundle bundle = RemoteOSGiServiceImpl.context
						.installBundle("file:" + bundleLocation);

				System.out.println("BUNDLE LOCATION: " + bundleLocation);

				// store the bundle for state updates and cleanup
				proxyBundles.put(service.getFragment(), bundle);

				// start the bundle
				bundle.start();

				// delay until the services are available
				// TODO: use service tracker
			} else {
				final DeliverBundleMessage delivB = (DeliverBundleMessage) msg;
				bundleLocation = "streamed";
				Bundle bundle = RemoteOSGiServiceImpl.context.installBundle(ref
						.getURI().toString(), new ByteArrayInputStream(delivB
						.getBytes()));
				bundle.start();
			}
		} catch (BundleException e) {
			final Throwable nested = e.getNestedException() == null ? e : e
					.getNestedException();
			nested.printStackTrace();
			throw new RemoteOSGiException(
					"Could not install the generated bundle " + bundleLocation,
					nested);
		}

	}

	private void send(final RemoteOSGiMessage msg) {
		if (networkChannel == null) {
			return;
		}

		if (msg.getXID() == 0) {
			msg.setXID(RemoteOSGiServiceImpl.nextXid());
		}

		try {
			try {
				networkChannel.sendMessage(msg);
				return;
			} catch (IOException ioe) {
				if (!reconnecting) {
					reconnecting = true;
					networkChannel.reconnect();
					reconnecting = false;
				} else {
					// TODO: enqueue messages and retransmit if recovery
					// succeeds.
				}
				// TimeOffsetMessages have to be handled differently
				// must send a new message with a new timestamp and XID
				// instead
				// of sending the same message again
				if (msg instanceof TimeOffsetMessage) {
					((TimeOffsetMessage) msg).restamp(RemoteOSGiServiceImpl
							.nextXid());
					networkChannel.sendMessage(msg);
				} else {
					networkChannel.sendMessage(msg);
				}
			}
		} catch (IOException ioe) {
			// failed to reconnect...
			dispose();
			throw new RemoteOSGiException("Network error", ioe);
		}
	}

	private RemoteOSGiMessage sendMessage(final RemoteOSGiMessage msg) {
		if (msg.getXID() == 0) {
			msg.setXID(RemoteOSGiServiceImpl.nextXid());
		}
		final Integer xid = new Integer(msg.getXID());
		synchronized (receiveQueue) {
			receiveQueue.put(xid, WAITING);
		}

		send(msg);

		// wait for the reply
		Object reply;
		synchronized (receiveQueue) {
			reply = receiveQueue.get(xid);
			final long timeout = System.currentTimeMillis() + TIMEOUT;
			try {
				while (!reconnecting && reply == WAITING
						&& System.currentTimeMillis() < timeout) {
					receiveQueue.wait(TIMEOUT);
					reply = receiveQueue.get(xid);
				}
				receiveQueue.remove(xid);

				if (reconnecting) {
					throw new RemoteOSGiException("Lost connection");
				} else if (reply == WAITING) {
					throw new RemoteOSGiException(
							"Method Invocation failed, timeout exceeded.");
				} else {
					return (RemoteOSGiMessage) reply;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	/**
	 * get the temporal offset of a remote peer.
	 * 
	 * @return the TimeOffset.
	 * @throws RemoteOSGiException
	 *             in case of network errors.
	 */
	public TimeOffset getOffset() throws RemoteOSGiException {
		if (timeOffset == null) {
			// if unknown, perform a initial offset measurement round of 4
			// messages
			TimeOffsetMessage timeMsg = new TimeOffsetMessage();
			for (int i = 0; i < 4; i++) {
				timeMsg.timestamp();
				timeMsg = (TimeOffsetMessage) sendMessage(timeMsg);
			}
			timeOffset = new TimeOffset(timeMsg.getTimeSeries());
		} else if (timeOffset.isExpired()) {
			// if offset has expired, start a new measurement round
			TimeOffsetMessage timeMsg = new TimeOffsetMessage();
			for (int i = 0; i < timeOffset.seriesLength(); i += 2) {
				timeMsg.timestamp();
				timeMsg = (TimeOffsetMessage) sendMessage(timeMsg);
			}
			timeOffset.update(timeMsg.getTimeSeries());
		}
		return timeOffset;
	}

	boolean isActive(final String uri) {
		return remoteServices.get(uri) != null;
	}

	private RemoteServiceRegistration getService(final String serviceID) {
		final RemoteServiceRegistration reg = RemoteOSGiServiceImpl
				.getService(serviceID);

		if (reg == null) {
			throw new IllegalStateException("Could not get " + serviceID);
		}
		if (reg instanceof ProxiedServiceRegistration) {
			localServices.put(serviceID, reg);
		}
		return reg;

	}

	private void populateLease(final LeaseMessage lease,
			final RemoteServiceRegistration[] regs, final String[] topics) {
		final String[] serviceIDs = new String[regs.length];
		final String[][] serviceInterfaces = new String[regs.length][];
		final Dictionary[] serviceProperties = new Dictionary[regs.length];

		for (short i = 0; i < regs.length; i++) {
			serviceIDs[i] = String.valueOf(regs[i].getServiceID());
			serviceInterfaces[i] = regs[i].getInterfaceNames();
			serviceProperties[i] = regs[i].getProperties();
		}
		lease.setServiceIDs(serviceIDs);
		lease.setServiceInterfaces(serviceInterfaces);
		lease.setServiceProperties(serviceProperties);
		lease.setTopics(topics);
	}

	private RemoteServiceReference[] processLease(final LeaseMessage lease) {
		final String[] serviceIDs = lease.getServiceIDs();
		final String[][] serviceInterfaces = lease.getServiceInterfaces();
		final Dictionary[] serviceProperties = lease.getServiceProperties();

		final RemoteServiceReferenceImpl[] refs = new RemoteServiceReferenceImpl[serviceIDs.length];
		for (short i = 0; i < serviceIDs.length; i++) {
			refs[i] = new RemoteServiceReferenceImpl(serviceInterfaces[i],
					serviceIDs[i], serviceProperties[i], this);

			remoteServices.put(refs[i].getURI().toString(), refs[i]);
			RemoteOSGiServiceImpl
					.notifyRemoteServiceListeners(new RemoteServiceEvent(
							RemoteServiceEvent.REGISTERED, refs[i]));
		}
		updateTopics(lease.getTopics(), new String[0]);
		return refs;
	}

	/**
	 * update the statements of supply and demand.
	 * 
	 * @param lease
	 *            the original lease.
	 */
	private void updateTopics(final String[] topicsAdded,
			final String[] topicsRemoved) {

		if (handlerReg == null) {
			if (topicsAdded.length > 0) {
				// register handler
				final Dictionary properties = new Hashtable();
				properties.put(EventConstants.EVENT_TOPIC, topicsAdded);
				properties.put(EventConstants.EVENT_FILTER, NO_LOOPS);
				properties.put(RemoteOSGiServiceImpl.R_OSGi_INTERNAL,
						Boolean.TRUE);
				handlerReg = RemoteOSGiServiceImpl.context.registerService(
						EventHandler.class.getName(), new EventForwarder(),
						properties);
				remoteTopics.addAll(Arrays.asList(topicsAdded));
			}
		} else {
			remoteTopics.removeAll(Arrays.asList(topicsRemoved));
			remoteTopics.addAll(Arrays.asList(topicsAdded));

			if (remoteTopics.size() == 0) {
				// unregister handler
				handlerReg.unregister();
				handlerReg = null;
			} else {
				// update topics
				final Dictionary properties = new Hashtable();
				properties.put(EventConstants.EVENT_TOPIC,
						(String[]) remoteTopics.toArray(new String[remoteTopics
								.size()]));
				properties.put(EventConstants.EVENT_FILTER, NO_LOOPS);
				properties.put(RemoteOSGiServiceImpl.R_OSGi_INTERNAL,
						Boolean.TRUE);
				handlerReg.setProperties(properties);
			}
		}

		if (RemoteOSGiServiceImpl.MSG_DEBUG) {
			RemoteOSGiServiceImpl.log.log(LogService.LOG_DEBUG,
					"NEW REMOTE TOPIC SPACE for " + getRemoteEndpoint()
							+ " is " + remoteTopics);
		}
	}

	/**
	 * message handler method.
	 * 
	 * @param msg
	 *            the incoming message.
	 * @return if reply is created, null otherwise.
	 * @throws RemoteOSGiException
	 *             if something goes wrong.
	 */
	private RemoteOSGiMessage handleMessage(final RemoteOSGiMessage msg)
			throws RemoteOSGiException {

		switch (msg.getFuncID()) {
		// requests
		case RemoteOSGiMessage.LEASE: {
			final LeaseMessage lease = (LeaseMessage) msg;
			processLease(lease);

			populateLease(lease, RemoteOSGiServiceImpl.getServices(),
					RemoteOSGiServiceImpl.getTopics());
			return lease;
		}
		case RemoteOSGiMessage.FETCH_SERVICE: {
			try {
				final FetchServiceMessage fetchReq = (FetchServiceMessage) msg;
				final String serviceID = fetchReq.getServiceID();

				final RemoteServiceRegistration reg = getService(serviceID);

				if (reg instanceof ProxiedServiceRegistration) {
					final DeliverServiceMessage m = ((ProxiedServiceRegistration) reg)
							.getDeliverServiceMessage();
					m.setXID(fetchReq.getXID());
					m.setServiceID(fetchReq.getServiceID());
					return m;
				} else if (reg instanceof BundledServiceRegistration) {
					DeliverBundleMessage m = new DeliverBundleMessage();
					m.setXID(msg.getXID());
					m.setBytes(((BundledServiceRegistration) reg).getBundle());
				}

			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		case RemoteOSGiMessage.LEASE_UPDATE: {
			LeaseUpdateMessage suMsg = (LeaseUpdateMessage) msg;

			final String serviceID = suMsg.getServiceID();
			final short stateUpdate = suMsg.getType();

			switch (stateUpdate) {
			case LeaseUpdateMessage.TOPIC_UPDATE: {
				updateTopics((String[]) suMsg.getPayload()[0], (String[]) suMsg
						.getPayload()[1]);
				return null;
			}
			case LeaseUpdateMessage.SERVICE_ADDED: {
				final RemoteServiceReferenceImpl ref = new RemoteServiceReferenceImpl(
						(String[]) suMsg.getPayload()[0], serviceID,
						(Dictionary) suMsg.getPayload()[1], this);

				remoteServices.put(getRemoteEndpoint().resolve("#" + serviceID)
						.toString(), ref);

				RemoteOSGiServiceImpl
						.notifyRemoteServiceListeners(new RemoteServiceEvent(
								RemoteServiceEvent.REGISTERED, ref));
				return null;
			}
			case LeaseUpdateMessage.SERVICE_MODIFIED: {
				final Dictionary newProps = (Dictionary) suMsg.getPayload()[1];
				final ServiceRegistration reg = (ServiceRegistration) proxiedServices
						.get(serviceID);
				if (reg != null) {
					reg.setProperties(newProps);
				}

				final RemoteServiceReferenceImpl ref = getRemoteReference(getRemoteEndpoint()
						.resolve("#" + serviceID).toString());
				ref.setProperties(newProps);
				RemoteOSGiServiceImpl
						.notifyRemoteServiceListeners(new RemoteServiceEvent(
								RemoteServiceEvent.MODIFIED, ref));
				return null;
			}
			case LeaseUpdateMessage.SERVICE_REMOVED: {
				// FIXME: hack
				if (networkChannel == null) {
					System.out.println("==============================");
					System.err.println("RECEIVED " + msg);
					System.err.println("WHILE CHANNEL IS NULL...");

					return null;
				}
				final Bundle bundle = (Bundle) proxyBundles.remove(serviceID);
				if (bundle != null) {
					try {
						bundle.uninstall();
					} catch (BundleException be) {
						be.printStackTrace();
					}
					proxiedServices.remove(serviceID);
					remoteServices.remove(getRemoteEndpoint().resolve(
							"#" + serviceID).toString());
				}
				final RemoteServiceReference ref = (RemoteServiceReference) remoteServices
						.remove(getRemoteEndpoint().resolve("#" + serviceID)
								.toString());
				// FIXME: why is this null?
				if (ref != null) {
					RemoteOSGiServiceImpl
							.notifyRemoteServiceListeners(new RemoteServiceEvent(
									RemoteServiceEvent.UNREGISTERING, ref));
				}
				return null;
			}
			}
		}
		case RemoteOSGiMessage.INVOKE_METHOD: {
			final InvokeMethodMessage invMsg = (InvokeMethodMessage) msg;
			try {
				ProxiedServiceRegistration serv = (ProxiedServiceRegistration) localServices
						.get(invMsg.getServiceID());
				if (serv == null) {
					final RemoteServiceRegistration reg = getService(invMsg
							.getServiceID());
					if (reg == null
							|| !(reg instanceof ProxiedServiceRegistration)) {
						throw new IllegalStateException(toString()
								+ "Could not get " + invMsg.getServiceID()
								+ ", known services " + localServices);
					} else {
						serv = (ProxiedServiceRegistration) reg;
					}
				}

				// get the invocation arguments and the local method
				final Object[] arguments = invMsg.getArgs();
				final Method method = serv.getMethod(invMsg
						.getMethodSignature());

				// invoke method
				try {
					final Object result = method.invoke(
							serv.getServiceObject(), arguments);
					final MethodResultMessage m = new MethodResultMessage();
					m.setXID(invMsg.getXID());
					m.setResult(result);
					return m;
				} catch (InvocationTargetException t) {
					throw t.getTargetException();
				}
			} catch (Throwable t) {
				final MethodResultMessage m = new MethodResultMessage();
				m.setXID(invMsg.getXID());
				m.setException(t);
				return m;
			}
		}
		case RemoteOSGiMessage.REMOTE_EVENT: {
			final RemoteEventMessage eventMsg = (RemoteEventMessage) msg;

			final Event event = new Event(eventMsg.getTopic(), eventMsg
					.getProperties());

			// and deliver the event to the local framework
			if (RemoteOSGiServiceImpl.eventAdminTracker.getTrackingCount() > 0) {
				((EventAdmin) RemoteOSGiServiceImpl.eventAdminTracker
						.getService()).postEvent(event);
			} else {
				// TODO: to log
				System.err.println("Could not deliver received event: " + event
						+ ". No EventAdmin available.");
			}
			return null;
		}
		case RemoteOSGiMessage.TIME_OFFSET: {
			// add timestamp to the message and return the message to sender
			((TimeOffsetMessage) msg).timestamp();
			return msg;
		}
		default:
			throw new RemoteOSGiException("Unimplemented message " + msg);
		}
	}

	/**
	 * forwards events over the channel to the remote peer.
	 * 
	 * @author Jan S. Rellermeyer, ETH Zurich
	 * @category EventHandler
	 */
	private final class EventForwarder implements EventHandler {

		public void handleEvent(final Event event) {
			try {
				final RemoteEventMessage msg = new RemoteEventMessage();
				msg.setTopic(event.getTopic());
				final String[] propertyNames = event.getPropertyNames();
				final Dictionary props = new Hashtable();
				for (int i = 0; i < propertyNames.length; i++) {
					props.put(propertyNames[i], event
							.getProperty(propertyNames[i]));
				}
				props.put(RemoteEventMessage.EVENT_SENDER_URI,
						getLocalEndpoint());
				msg.setProperties(props);
				send(msg);

				if (RemoteOSGiServiceImpl.MSG_DEBUG) {
					RemoteOSGiServiceImpl.log.log(LogService.LOG_DEBUG,
							"Forwarding Event " + event);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public boolean isConnected() {
		return networkChannel != null;
	}

	public String toString() {
		return "ChannelEndpoint(" + networkChannel.toString() + ")";
	}
}
