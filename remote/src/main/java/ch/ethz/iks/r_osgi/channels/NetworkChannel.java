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
package ch.ethz.iks.r_osgi.channels;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;

import ch.ethz.iks.r_osgi.RemoteOSGiMessage;

/**
 * <p>
 * Interface for all transport channel classes. Implementations of this
 * interface are typically returned by services that offer the
 * <code>TransportChannelFactory</code> service.
 * </p>
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 * @since 0.6
 */
public interface NetworkChannel {

	/**
	 * get the protocol that this channel uses on the transport layer.
	 * 
	 * @return the protocol identifier as <code>String</code>. Should be in
	 *         lowercase.
	 * @since 0.6
	 */
	String getProtocol();

	/**
	 * get the address that the channel is connected to.
	 * 
	 * @return the address.
	 * @since 0.6
	 */
//	InetAddress getInetAddress();

	/**
	 * get the port that the channel is connected to.
	 * 
	 * @return the port.
	 * @since 0.6
	 */
//	int getPort();

	/**
	 * get the URI of the remote endpoint.
	 * 
	 * @return the ID.
	 */
	URI getRemoteEndpoint();

	/**
	 * get the URI of the local endpoint.
	 * 
	 * @return the ID.
	 */
	URI getLocalEndpoint();
	
	
	/**
	 * reconnect the channel to the remote endpoint.
	 * 
	 * @throws IOException
	 *             if the channel cannot be reconnected.
	 * @since 0.6
	 */
	void reconnect() throws IOException;

	/**
	 * send a message through the channel.
	 * 
	 * @param message
	 *            the message to be sent.
	 * @throws IOException
	 *             if the transport fails.
	 * @since 0.6
	 */
	void sendMessage(final RemoteOSGiMessage message) throws IOException;



}
