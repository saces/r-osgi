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
import java.util.Dictionary;

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
	private String[][] serviceInterfaces;

	private String[] fragments;

	private Dictionary[] serviceProperties;

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
	public LeaseMessage(final RemoteServiceRegistration[] regs, final String[] topics) {
		this.funcID = LEASE;
		parseRegistrations(regs);
		this.topics = topics == null ? new String[0] : topics;
	}

	/**
	 * creates a new LeaseMessage from network packet.
	 * 
	 * <pre>
	 *           0                   1                   2                   3
	 *           0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *          +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *          |       R-OSGi header (function = Lease = 1)                    |
	 *          +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *          |  Array of service info (Fragment#, Interface[], properties    \
	 *          +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *          |  Array of topic strings                                       \
	 *          +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
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
		final int serviceCount = input.readShort();	
		fragments = new String[serviceCount];
		serviceInterfaces = new String[serviceCount][];
		serviceProperties = new Dictionary[serviceCount];
		try {
			for (short i = 0; i < serviceCount; i++) {
				fragments[i] = input.readUTF();
				serviceInterfaces[i] = readStringArray(input);				
				serviceProperties[i] = (Dictionary) input.readObject();
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		this.topics = readStringArray(input);
	}

	/**
	 * get the services.
	 * 
	 * @return the services.
	 */
	RemoteServiceReferenceImpl[] getServices(final ChannelEndpointImpl channel) {
		final RemoteServiceReferenceImpl[] refs = new RemoteServiceReferenceImpl[fragments.length];
		for (short i = 0; i < fragments.length; i++) {
			refs[i] = new RemoteServiceReferenceImpl(serviceInterfaces[i], 
					fragments[i], serviceProperties[i], channel);
		}
		return refs;
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
	LeaseMessage replyWith(final RemoteServiceRegistration[] refs, final String[] topics) {
		parseRegistrations(refs);
		this.topics = topics;
		return this;
	}

	/**
	 * write the bytes of the message.
	 * 
	 * @param out
	 *            the output stream
	 * @throws IOException
	 *             in case of IO errors.
	 * @see ch.ethz.iks.r_osgi.impl.RemoteOSGiMessageImpl#getBody()
	 */
	public void writeBody(final ObjectOutputStream out) throws IOException {
		final int slen = serviceInterfaces.length;
		out.writeShort(slen);
		for (short i = 0; i < slen; i++) {
			out.writeUTF(fragments[i]);
			writeStringArray(out, serviceInterfaces[i]);			
			// TODO: smart serializer
			out.writeObject(serviceProperties[i]);
		}
		writeStringArray(out, topics);
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
		buffer.append(", services: ");
		for (int i = 0; i < serviceInterfaces.length; i++) {
			buffer.append(Arrays.asList(serviceInterfaces[i]));
			buffer.append("-");
			buffer.append(fragments[i]);
			if (i < serviceInterfaces.length) {
				buffer.append(", ");
			}
		}
		buffer.append(", topics: ");
		buffer.append(Arrays.asList(topics));
		return buffer.toString();
	}

	private void parseRegistrations(
			final RemoteServiceRegistration[] regs) {
		fragments = new String[regs.length];
		serviceInterfaces = new String[regs.length][];
		serviceProperties = new Dictionary[regs.length];
		for (short i = 0; i < regs.length; i++) {
			fragments[i] = String.valueOf(regs[i].getServiceID());
			serviceInterfaces[i] = regs[i].getInterfaceNames();
			serviceProperties[i] = regs[i].getProperties();
		}
	}
}
