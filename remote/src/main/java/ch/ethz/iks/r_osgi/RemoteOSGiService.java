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
package ch.ethz.iks.r_osgi;

import java.net.InetAddress;
import org.osgi.framework.ServiceReference;
import ch.ethz.iks.slp.ServiceURL;

/**
 * <p>
 * RemoteOSGiService provides transparent access to services on remote service
 * platforms. It uses SLP as underlying discovery protocol. Local services can
 * be registered for remoting, applications can register listeners for
 * <code>ServiceTypes</code> to be informed whenever matching services have
 * been discovered.
 * </p>
 * <p>
 * As soon as a service has been discovered and the listener has been informed,
 * the application can fetch the service. In the default case, the service
 * interface is transferred to the receiving peer together with an optional
 * smart proxy class and optional injections. The service then builds a proxy
 * bundle and registers it with the local framework so that the application can
 * get a service reference as if the service was local. Internally, all methods
 * of the service interface are implemented as remote method calls.
 * </p>
 * <p>
 * Services can define smart proxies to move some parts of the code to the
 * client. This is done by an abstract class. All implemented method will be
 * executed on the client, abstract methods will be implemented by remote method
 * calls. Moving parts of the code to the client can be useful for saving
 * service provider platform's resources.
 * </p>
 * <p>
 * Injections are used if the service interface uses classes as method arguments
 * that are not expected to be present on client side. These classes will be
 * automatically injected into the proxy bundle. The registrator can manually
 * inject additional classes.
 * </p>
 * <p>
 * With version 0.5, there is also the possibility to register a service with
 * the MIGRATE_BUNDLE policy. In this case, the bundle that provides the service
 * is moved to the requesting peer.
 * </p>
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 * @since 0.1
 */
public interface RemoteOSGiService {

	// public constants for service registrations

	/**
	 * this property has to be set in order to release a service for remote
	 * access. Currently, the following two policies are supported.
	 * 
	 * @since 0.5
	 */
	String R_OSGi_REGISTRATION = "service.remote.registration";

	/**
	 * policy "use_proxy" means: dynamically build a proxy at client side.
	 * (default)
	 * 
	 * @since 0.5
	 */
	String USE_PROXY_POLICY = "use_proxy";

	/**
	 * policy "migrate_bundle" transfers the whole bundle to the remote peer. In
	 * other words, it creates a copy of the bundle containing the service and
	 * installs this bundle on the other side.
	 * 
	 * @since 0.5
	 */
	String MIGRATE_BUNDLE_POLICY = "transfer_bundle";

	/**
	 * the auxiliary header entry for bundles running on strange frameworks that
	 * mess up the bundle location so that R-OSGi cannot grab the bundle from
	 * there.
	 * 
	 * @since 0.5
	 */
	String BUNDLE_URL = "bundle_url";

	/**
	 * Can be set to use a smart proxy. Smart proxies have to be abstract
	 * classes implementing the service interface. All abstract methods are
	 * implemented as remote calls, implemented methods remain untouched. This
	 * allows to perform some of the work on client side (inside of implemented
	 * methods). The value of this property in the service property dictionary
	 * has to be a <code>Class</code> object.
	 * 
	 * @since 0.5
	 */
	String SMART_PROXY = "service.remote.smartproxy";

	/**
	 * For special purposes, the service can decide to inject other classes into
	 * the proxy bundle that is dynamically created on the client side. For
	 * instance, if types are use as arguments of method calls that are not part
	 * of the standard execution environment and the service does not want to
	 * rely on assumption that the corresponding classes are present on client
	 * side, it can inject these classes. The value of this property in the
	 * service property dictionary has to be an array of <code>Class</code>
	 * objects.
	 * 
	 * @since 0.5
	 */
	String INJECTIONS = "service.remote.injections";

	/**
	 * property for registration of a service UI component that gived the user a
	 * presentation of the service. The value of the property in the service
	 * property dictionary has to be a <code>Class</code> object of a class
	 * implementing
	 * <code>org.service.proposition.remote.ServiceUIComponent</code>. When
	 * this property is set, the presentation is injected into the bundle and
	 * the R-OSGi ServiceUI can display the presentation when the service is
	 * discovered.
	 * 
	 * @since 0.5
	 */
	String PRESENTATION = "service.presentation";

	/**
	 * the property key for the host name of the remote service. This constant
	 * is set by R-OSGi when a service is transferred to a remote peer. So to
	 * find out whether a service is provided by an R-OSGi proxy, check for the
	 * presence of this key in the service properties.
	 * 
	 * @since 0.4
	 */
	String REMOTE_HOST = "service.remote.host";

	/**
	 * register a legacy service for remoting. For remote-aware services, simply
	 * set the <code>service.remote.registration</code> to a valid policy and
	 * the service is registered for remote access according to the policy. If
	 * the policy is invalid, the default policy (USE_PROXY) is taken.
	 * 
	 * @param service
	 *            the service reference
	 * @param policy
	 *            a policy name.
	 * @param smartProxy
	 *            optionally, the name of a smart proxy class.
	 * @param injections
	 *            optionally, the names of injection classes.
	 * @return an array of service urls. One for each interface of the service.
	 * @throws RemoteOSGiException
	 *             if the registration fails.
	 * @since 0.5
	 */
	ServiceURL[] registerService(final ServiceReference service,
			final String policy, final String smartProxy,
			final String[] injections) throws RemoteOSGiException;;

	/**
	 * unregister a service for remoting. This method is only for one special
	 * purpose: In case you want to unregister a service from remote access but
	 * not from the local framework, use this method. Otherwise, simply shut
	 * down the service and the remote registration will also disappear.
	 * 
	 * @param service
	 *            the <code>ServiceURL</code> that was returned as result of
	 *            the registration.
	 * @since 0.5
	 */
	void unregisterService(final ServiceURL service);

	/**
	 * connect to a remote OSGi framework. Has to be called prior to any service
	 * access. Causes the frameworks to exchange leases and start the transport
	 * of remote events.
	 * 
	 * @param host
	 *            the address of the remote framework host.
	 * @param port
	 *            the port of the remote framework host. Typically the R-OSGi
	 *            standard port 9278 but can also be something different if a
	 *            special TransportChannelFactory is used.
	 * @param protocol
	 *            the protocol string that maps to a protocol property of a
	 *            registered TransportChannelFactory. If <code>null</code>,
	 *            the default TCPChannel is used.
	 * @return the array of ServiceURLs that the remote frameworks offers.
	 * @throws RemoteOSGiException
	 *             in case of connection errors.
	 * @since 0.6
	 */
	ServiceURL[] connect(final InetAddress host, final int port,
			final String protocol) throws RemoteOSGiException;

	/**
	 * fetch the discovered remote service. The service will be fetched from the
	 * service providing host and a proxy bundle is registered with the local
	 * framework.
	 * 
	 * @param service
	 *            the <code>ServiceURL</code>.
	 * @throws RemoteOSGiException
	 *             if the fetching fails.
	 * @since 0.5
	 */
	void fetchService(final ServiceURL service) throws RemoteOSGiException;

	/**
	 * get the service that has just been fetched. Only works if the service has
	 * been fetched in form of a proxy.
	 * 
	 * @param url
	 *            the service url.
	 * @return the service belonging to the service url or null, if no such
	 *         service is present.
	 */
	Object getFetchedService(final ServiceURL url);

	/**
	 * 
	 * @param url
	 * @return
	 */
	ServiceReference getFetchedServiceReference(final ServiceURL url);
	
	/**
	 * transform a timestamp into the peer's local time.
	 * 
	 * @param sender
	 *            the sender serviceURL.
	 * @param timestamp
	 *            the Timestamp.
	 * @return the transformed timestamp.
	 * @throws RemoteOSGiException
	 *             if the transformation fails.
	 * @since 0.2
	 */
	Timestamp transformTimestamp(ServiceURL sender, Timestamp timestamp)
			throws RemoteOSGiException;

	/**
	 * get my own IP.
	 * 
	 * @return the own InetAddress, to be consistent with the SLP layer.
	 * @since 0.4
	 */
	InetAddress getMyIP();

}
