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

import java.util.Dictionary;
import ch.ethz.iks.slp.ServiceURL;

/**
 * This interface is used by generated proxy bundles. Applications don't have to
 * use it.
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 * @since 0.1
 */
public interface Remoting {

	/**
	 * get the endpoint of the channel to the denoted service url. If the peer
	 * is not connected, it returns <code>null</code>.
	 * 
	 * @param serviceURL
	 *            the service url.
	 * @return the endpoint or <code>null</code>
	 * @since 0.6
	 */
	ChannelEndpoint getEndpoint(final String serviceURL);

	/**
	 * get the attributes of a service. This function is used to simplify proxy
	 * bundle generation.
	 * 
	 * @param serviceURL
	 *            the serviceURL of the remote service.
	 * @return the service attributes.
	 * @since 0.1
	 */
	Dictionary getAttributes(final String serviceURL);

	/**
	 * get the attributes for the presentation of the service. This function is
	 * used by proxies that support ServiceUI presentations.
	 * 
	 * @param serviceURL
	 *            the serviceURL of the remote service.
	 * @return the attributes for the presentation.
	 * @since 0.1
	 */
	Dictionary getPresentationAttributes(final String serviceURL);

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
	Timestamp transformTimestamp(final ServiceURL sender,
			final Timestamp timestamp) throws RemoteOSGiException;
}