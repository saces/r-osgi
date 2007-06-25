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
import java.net.InetAddress;
import java.net.Socket;
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
import ch.ethz.iks.r_osgi.RemoteOSGiMessage;
import ch.ethz.iks.r_osgi.RemoteOSGiException;
import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.RemoteServiceEvent;
import ch.ethz.iks.r_osgi.RemoteServiceReference;
import ch.ethz.iks.r_osgi.channels.ChannelEndpoint;
import ch.ethz.iks.r_osgi.channels.NetworkChannel;
import ch.ethz.iks.r_osgi.channels.NetworkChannelFactory;

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
	 * endpoint. List of RemoteServiceReferences
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
	 * map service url -> proxy bundle. If the endpoint is closed, the proxies
	 * are unregistered.
	 */
	private final HashMap proxyBundles = new HashMap(0);

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
			+ RemoteOSGiServiceImpl.EVENT_SENDER_URL + "=*))";

	/**
	 * the TCPChannel factory.
	 */
	private static final TCPChannelFactory TCP_FACTORY = new TCPChannelFactory();

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
			final InetAddress address, final int port, final String protocol)
			throws RemoteOSGiException, IOException {
		this.networkChannel = (factory == null ? TCP_FACTORY : factory)
				.getConnection(this, address, port, protocol);
		if (RemoteOSGiServiceImpl.DEBUG) {
			RemoteOSGiServiceImpl.log.log(LogService.LOG_DEBUG,
					"opening new channel " + getID());
		}
		RemoteOSGiServiceImpl.registerChannel(this);
	}

	/**
	 * create a new channel endpoint from an incoming connection. Incoming
	 * connections are always TCP connections, either directly by the remote
	 * peer or over localloop by a bridge.
	 * 
	 * @param socket
	 *            the socket on which the incoming connection was accepted.
	 * @throws IOException
	 *             if something goes wrong on the network layer.
	 */
	ChannelEndpointImpl(final Socket socket) throws IOException {
		this.networkChannel = TCP_FACTORY.bind(this, socket);
		RemoteOSGiServiceImpl.registerChannel(this);
	}

	RemoteServiceReference[] sendLease(
			final RemoteServiceRegistration[] myServices,
			final String[] myTopics) {
		final LeaseMessage lease = (LeaseMessage) sendMessage(new LeaseMessage(getID(),
				myServices, myTopics));

		return processLease(lease);
	}

	void updateLease(final LeaseUpdateMessage msg) {
		send(msg);
	}

	/**
	 * get the channel ID.
	 * 
	 * @return the channel ID.
	 * @see ch.ethz.iks.r_osgi.channels.ChannelEndpoint#getID() *
	 * @category ChannelEndpoint
	 */
	public String getID() {
		return networkChannel.getID();
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
						"Connection to " + getID()
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
				RemoteOSGiMessage reply = handleMessage(msg);
				if (reply != null) {
					try {
						networkChannel.sendMessage(reply);
					} catch (IOException e) {
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
		final InvokeMethodMessage invokeMsg = new InvokeMethodMessage(service
				.toString(), methodSignature, args);

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
					"DISPOSING ENDPOINT " + getID());
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
					bundles[i].uninstall();
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
	public Dictionary getProperties(final String url) {
		return getRemoteReference(url).getProperties();
	}

	private RemoteServiceReferenceImpl getRemoteReference(final String url) {
		return (RemoteServiceReferenceImpl) remoteServices.get(url);
	}

	/**
	 * 
	 * @param filter
	 * @return
	 */
	RemoteServiceReference[] getRemoteReferences(final Filter filter) {
		final List result = new ArrayList();
		final RemoteServiceReferenceImpl[] refs = (RemoteServiceReferenceImpl[]) remoteServices
				.keySet().toArray(
						new RemoteServiceReferenceImpl[remoteServices.size()]);
		for (int i = 0; i < refs.length; i++) {
			if (filter.match(refs[i].getProperties())) {
				result.add(refs);
			}
		}
		return (RemoteServiceReference[]) result
				.toArray(new RemoteServiceReference[result.size()]);
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
	public Dictionary getPresentationProperties(final String url) {
		final Dictionary attribs = new Hashtable();
		attribs.put(RemoteOSGiServiceImpl.SERVICE_URL, url);
		attribs.put(RemoteOSGiService.PRESENTATION, getRemoteReference(url)
				.getProperty(RemoteOSGiService.PRESENTATION));
		return attribs;
	}

	/**
	 * 
	 * @param url
	 * @param reg
	 * @category ChannelEndpoint
	 */
	public void trackRegistration(final String url,
			final ServiceRegistration reg) {
		proxiedServices.put(url, reg);
	}

	/**
	 * 
	 * @param serviceURL
	 * @category ChannelEndpoint
	 */
	public void untrackRegistration(final String url) {
		proxiedServices.remove(url);
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
			BundleException {

		// build the FetchServiceMessage
		final FetchServiceMessage fetchReq = new FetchServiceMessage(ref);

		// send the FetchServiceMessage and get a DeliverServiceMessage in
		// return
		final RemoteOSGiMessageImpl msg = sendMessage(fetchReq);
		if (msg instanceof DeliverServiceMessage) {
			final DeliverServiceMessage deliv = (DeliverServiceMessage) msg;
			final String url = deliv.getURL();

			// generate a proxy bundle for the service
			final String bundleLocation = new ProxyGenerator()
					.generateProxyBundle(url, getID(), deliv);

			// install the proxy bundle
			final Bundle bundle = RemoteOSGiServiceImpl.context
					.installBundle("file:" + bundleLocation);

			// store the bundle for state updates and cleanup
			proxyBundles.put(url, bundle);

			// start the bundle
			bundle.start();
		} else {
			final DeliverBundleMessage delivB = (DeliverBundleMessage) msg;

			Bundle bundle = RemoteOSGiServiceImpl.context.installBundle(ref
					.getURL(), new ByteArrayInputStream(delivB.getBundle()));
			bundle.start();
		}
	}

	private void send(final RemoteOSGiMessageImpl msg) {
		if (networkChannel == null) {
			return;
		}

		if (msg.xid == 0) {
			msg.xid = RemoteOSGiServiceImpl.nextXid();
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

	private RemoteOSGiMessageImpl sendMessage(final RemoteOSGiMessageImpl msg) {
		if (msg.xid == 0) {
			msg.xid = RemoteOSGiServiceImpl.nextXid();
		}
		final Integer xid = new Integer(msg.xid);
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
					return (RemoteOSGiMessageImpl) reply;
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
	TimeOffset getOffset() throws RemoteOSGiException {
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

	private RemoteServiceReference[] processLease(LeaseMessage lease) {
		final RemoteServiceReferenceImpl[] refs = lease.getServices(this);
		for (short i = 0; i < refs.length; i++) {
			remoteServices.put(refs[i].getURL(), refs[i]);
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
					"NEW REMOTE TOPIC SPACE for " + getID() + " is "
							+ remoteTopics);
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
		case RemoteOSGiMessageImpl.LEASE: {
			final LeaseMessage lease = (LeaseMessage) msg;
			processLease(lease);

			return lease.replyWith(getID(), RemoteOSGiServiceImpl.getServices(),
					RemoteOSGiServiceImpl.getTopics());
		}
		case RemoteOSGiMessageImpl.FETCH_SERVICE: {
			try {
				final FetchServiceMessage fetchReq = (FetchServiceMessage) msg;
				final String url = fetchReq.getURL();

				final RemoteServiceRegistration reg = RemoteOSGiServiceImpl
						.getService(fetchReq.getServiceID());

				if (reg == null) {
					throw new IllegalStateException("Could not get "
							+ fetchReq.getURL());
				}

				if (reg instanceof ProxiedServiceRegistration) {

					localServices.put(url, reg);

					RemoteOSGiMessage m = ((ProxiedServiceRegistration) reg)
							.deliver(fetchReq);

					return m;
				} else if (reg instanceof BundledServiceRegistration) {
					return new DeliverBundleMessage(fetchReq,
							(BundledServiceRegistration) reg);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		case RemoteOSGiMessageImpl.LEASE_UPDATE: {
			LeaseUpdateMessage suMsg = (LeaseUpdateMessage) msg;

			final String url = suMsg.getURL();
			final short stateUpdate = suMsg.getType();

			switch (stateUpdate) {
			case LeaseUpdateMessage.TOPIC_UPDATE: {
				updateTopics((String[]) suMsg.getContent()[0], (String[]) suMsg
						.getContent()[1]);
				return null;
			}
			case LeaseUpdateMessage.SERVICE_ADDED: {
				final RemoteServiceReferenceImpl ref = new RemoteServiceReferenceImpl(
						(String[]) suMsg.getContent()[0], url,
						(Dictionary) suMsg.getContent()[1], this);
				remoteServices.put(ref.getURL(), ref);

				RemoteOSGiServiceImpl
						.notifyRemoteServiceListeners(new RemoteServiceEvent(
								RemoteServiceEvent.REGISTERED, ref));
				return null;
			}
			case LeaseUpdateMessage.SERVICE_MODIFIED: {
				final Dictionary newProps = (Dictionary) suMsg.getContent()[1];
				final ServiceRegistration reg = (ServiceRegistration) proxiedServices
						.get(url);
				if (reg != null) {
					reg.setProperties(newProps);
				}

				final RemoteServiceReferenceImpl ref = getRemoteReference(url);
				ref.setProperties(newProps);
				RemoteOSGiServiceImpl
						.notifyRemoteServiceListeners(new RemoteServiceEvent(
								RemoteServiceEvent.MODIFIED, ref));
				return null;
			}
			case LeaseUpdateMessage.SERVICE_REMOVED: {
				final Bundle bundle = (Bundle) proxyBundles.remove(url);
				if (bundle != null) {
					try {
						bundle.uninstall();
					} catch (BundleException be) {
						be.printStackTrace();
					}
					proxiedServices.remove(url);
				}
				// TODO: remove debug output
				System.out.println("URL " + url);
				System.out.println("REFS " + remoteServices);
				final RemoteServiceReference ref = (RemoteServiceReference) remoteServices
						.remove(url);
				RemoteOSGiServiceImpl
						.notifyRemoteServiceListeners(new RemoteServiceEvent(
								RemoteServiceEvent.UNREGISTERING, ref));
				return null;
			}
			}
		}
		case RemoteOSGiMessageImpl.INVOKE_METHOD: {
			final InvokeMethodMessage invMsg = (InvokeMethodMessage) msg;
			try {
				final ProxiedServiceRegistration serv = (ProxiedServiceRegistration) localServices
						.get(invMsg.getURL());
				if (serv == null) {
					throw new IllegalStateException("Could not get "
							+ invMsg.getURL() + ", known services "
							+ localServices);
				}

				// get the invokation arguments and the local method
				final Object[] arguments = invMsg.getArgs();
				final Method method = serv.getMethod(invMsg
						.getMethodSignature());

				// invoke method
				try {
					final Object result = method.invoke(
							serv.getServiceObject(), arguments);
					return new MethodResultMessage(invMsg, result);
				} catch (InvocationTargetException t) {
					throw t.getTargetException();
				}
			} catch (Throwable t) {
				return new MethodResultMessage(invMsg, t);
			}
		}
		case RemoteOSGiMessageImpl.REMOTE_EVENT: {
			final RemoteEventMessage eventMsg = (RemoteEventMessage) msg;

			final Event event = eventMsg.getEvent(this);

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
		case RemoteOSGiMessageImpl.TIME_OFFSET: {
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
				send(new RemoteEventMessage(event, getID()));
				if (RemoteOSGiServiceImpl.MSG_DEBUG) {
					RemoteOSGiServiceImpl.log.log(LogService.LOG_DEBUG,
							"Forwarding Event " + event);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
