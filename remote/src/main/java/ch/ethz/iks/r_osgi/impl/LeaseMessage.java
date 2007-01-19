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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.Arrays;

import ch.ethz.iks.slp.ServiceLocationException;
import ch.ethz.iks.slp.ServiceURL;

/**
 * Lease message. Is exchanged when a channel is established. Leases are the
 * implementations of the statements of supply and demand.
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 * @since 0.6
 */
final class LeaseMessage extends RemoteOSGiMessageImpl {

	/**
	 * the services that the peer offers.
	 */
	private String[] services;

	/**
	 * the event topics that the peer is interested in.
	 */
	private String[] topics;

	/**
	 * create a new lease message.
	 * 
	 * @param services
	 *            the services the peer offers.
	 * @param topics
	 *            the topics the peer is interested in.
	 */
	public LeaseMessage(final String[] services, final String[] topics) {
		this.funcID = LEASE;
		this.services = services;
		this.topics = topics;
	}

	/**
	 * creates a new LeaseMessage from network packet.
	 * 
	 * <pre>
	 *         0                   1                   2                   3
	 *         0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *        |       R-OSGi header (function = Fetch = 1)                    |
	 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *        |  Array of service strings                                     \
	 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *        |  Array of topic strings                                       \
	 *        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * </pre>
	 * 
	 * @param input
	 *            an <code>ObjectInputStream</code> that provides the body of
	 *            a R-OSGi network packet.
	 * @throws IOException
	 *             if something goes wrong.
	 */
	LeaseMessage(final ObjectInputStream input) throws IOException {
		funcID = LEASE;
		final int slen = input.readShort();
		System.out.println(slen);
		final String[] services = new String[slen];
		for (int i = 0; i < slen; i++) {
			services[i] = input.readUTF();
		}
		this.services = services;

		final int tlen = input.readShort();
		final String[] topics = new String[tlen];
		for (int i = 0; i < tlen; i++) {
			topics[i] = input.readUTF();
		}
		this.topics = topics;
	}

	/**
	 * get the services.
	 * 
	 * @return the services.
	 */
	String[] getServices() {
		return services;
	}

	/**
	 * get the topics.
	 * 
	 * @return the topics.
	 */
	String[] getTopics() {
		return topics;
	}

	/**
	 * reply to this message by publishing the peers own services and topic
	 * interests.
	 * 
	 * @param services
	 *            the services of this peer.
	 * @param topics
	 *            the topics of interest of this peer.
	 * @return the reply lease message.
	 */
	LeaseMessage replyWith(final String[] services, final String[] topics) {
		this.services = services;
		this.topics = topics;
		return this;
	}

	/**
	 * get the bytes of the message.
	 * 
	 * @return the bytes.
	 * @throws IOException
	 *             in case of IO errors.
	 * @see ch.ethz.iks.r_osgi.impl.RemoteOSGiMessageImpl#getBody()
	 */
	public void writeBody(final ObjectOutputStream out) throws IOException {
		final int slen = services.length;
		out.writeShort(slen);
		for (int i = 0; i < slen; i++) {
			out.writeUTF(services[i]);
		}

		final int tlen = topics.length;
		out.writeShort(tlen);
		for (int i = 0; i < tlen; i++) {
			out.writeUTF(topics[i]);
		}
	}

	/**
	 * restamp the service URL to a new address.
	 * 
	 * @param protocol
	 *            the protocol.
	 * @param host
	 *            the host.
	 * @param port
	 *            the port.
	 * @throws ServiceLocationException
	 * @see ch.ethz.iks.r_osgi.RemoteOSGiMessage#restamp(java.lang.String,
	 *      java.lang.String, int)
	 */
	public void restamp(final String protocol, final String host, final int port)
			throws ServiceLocationException {
		for (int i = 0; i < services.length; i++) {
			final ServiceURL original = new ServiceURL(services[i], 0);
			final ServiceURL restamped = new ServiceURL(original
					.getServiceType()
					+ "://"
					+ (protocol != null ? (protocol + "://") : "")
					+ host + ":" + port + original.getURLPath(), 0);
			services[i] = restamped.toString();
		}
	}

	/**
	 * String representation for debug outputs.
	 * 
	 * @return a string representation.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[LEASE] - XID: ");
		buffer.append(xid);
		buffer.append(", urls: ");
		buffer.append(Arrays.asList(services));
		buffer.append(", topics: ");
		buffer.append(Arrays.asList(topics));
		return buffer.toString();
	}
}
