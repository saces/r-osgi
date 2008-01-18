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
package ch.ethz.iks.r_osgi.messages;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.Dictionary;
import ch.ethz.iks.util.SmartSerializer;

/**
 * <p>
 * RemoteEventMessage transfers RemoteEvents via multicast to all listening
 * peers.
 * </p>
 * 
 * @author Jan S. Rellermeyer, ETH Zürich
 * @since 0.2
 */
public final class RemoteEventMessage extends RemoteOSGiMessage {

	/**
	 * the event property contains the sender's uri.
	 */
	public static final String EVENT_SENDER_URI = "sender.uri";

	/**
	 * 
	 * the event topic. E.g. <code>ch.ethz.iks.SampleEvent</code>.
	 */
	private String topic;

	/**
	 * the marshalled properties of the event.
	 */
	private Dictionary properties;

	/**
	 * creates a new RemoteEventMessage from RemoteEvent.
	 * 
	 * @param event
	 *            the remote event.
	 * @param channelID
	 *            the ID of the channel through which the event is delivered.
	 * @throws Exception
	 *             if the destination address of the event cannot be resolved or
	 *             if marshalling of the event fails.
	 */
	public RemoteEventMessage() throws Exception {
		super(REMOTE_EVENT);
	}

	/**
	 * creates a new RemoteEventMessage from network packet:
	 * 
	 * <pre>
	 *          0                   1                   2                   3
	 *          0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *         |       R-OSGi header (function = RemoteEvent = 5)              |
	 *         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *         | length of &lt;topic&gt;           |   &lt;topic&gt; String                \
	 *         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *         | Properties Dictionary MarshalledObject                        \
	 *         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * </pre>.
	 * 
	 * @param input
	 *            an <code>ObjectInputStream</code> that provides the body of
	 *            a R-OSGi network packet.
	 * @throws IOException
	 *             in case of IO failures.
	 */
	RemoteEventMessage(final ObjectInputStream input) throws IOException {
		super(REMOTE_EVENT);
		topic = input.readUTF();
		properties = (Dictionary) SmartSerializer.deserialize(input);
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(final String topic) {
		this.topic = topic;
	}

	public Dictionary getProperties() {
		return properties;
	}

	public void setProperties(final Dictionary properties) {
		this.properties = properties;
	}

	/*
	 * Event getEvent(final ChannelEndpointImpl connection) throws
	 * RemoteOSGiException { final Long remoteTs; if ((remoteTs = (Long)
	 * properties.get(EventConstants.TIMESTAMP)) != null) { // transform the
	 * event timestamps properties.put(EventConstants.TIMESTAMP,
	 * connection.getOffset() .transform(remoteTs)); } return new Event(topic,
	 * properties); }
	 */

	/**
	 * convenience methot to get the sender.
	 * 
	 * @return the sender URI as string.
	 */
	String getSender() {
		return (String) properties.get(EVENT_SENDER_URI);
	}

	/**
	 * write the body of the message to a stream.
	 * 
	 * @param out
	 *            the ObjectOutputStream.
	 * @throws IOException
	 *             in case of IO failures.
	 */
	public void writeBody(final ObjectOutputStream out) throws IOException {
		out.writeUTF(topic);
		SmartSerializer.serialize(properties, out);
	}

	/**
	 * String representation for debug outputs.
	 * 
	 * @return a string representation.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[REMOTE_EVENT] - XID: ");
		buffer.append(xid);
		buffer.append("topic: ");
		buffer.append(topic);
		return buffer.toString();
	}
}
