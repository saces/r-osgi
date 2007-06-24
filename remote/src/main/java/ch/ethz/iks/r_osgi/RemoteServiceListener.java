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

import java.util.EventListener;

/**
 * <p>
 * DiscoveryListener interface is used by applications to register for
 * discovered services. From release 0.5.0, this listener is registered using
 * the whiteboard pattern, i.e., by registering the implementation as service
 * under the DiscoveryListener interface.
 * </p>
 * 
 * @author Jan S. Rellermeyer, ETH Zürich
 * @since 0.1
 */
public interface RemoteServiceListener extends EventListener {

	/**
	 * Name of the property which denotes the service interfaces in which the
	 * listener is interested. Must be set to a <code>String</code> array of
	 * fully qualified names of the interfaces.
	 * 
	 * @since 0.6
	 */
	String SERVICE_INTERFACES = "listener.service_interfaces";

	/**
	 * Name of the property which denotes the filter for the listener. Only
	 * services matching the filter are announced to the listener, if this
	 * property is set to a filter <code>String</code>.
	 * 
	 * @since 0.6
	 */
	String FILTER = "listener.filter";

	/**
	 * if this property is set (to anything), the service is automatically
	 * fetched before the listener is called.
	 * 
	 * @since 0.5
	 */
	String AUTO_FETCH = "listener.auto_fetch";

	/**
	 * <p>
	 * notify the application that a remote service matching the constraints has
	 * been located.
	 * </p>
	 * <p>
	 * As soon as the service has been discovered and this method has been
	 * called, the application can call {@link RemoteOSGiService}#fetchService(ServiceURL)}
	 * to get a local proxy object for the remote service.
	 * </p>
	 * 
	 * @param service
	 *            the <code>ServiceURL</code> of the discovered service.
	 */
	void remoteServiceEvent(final RemoteServiceEvent event);

}
