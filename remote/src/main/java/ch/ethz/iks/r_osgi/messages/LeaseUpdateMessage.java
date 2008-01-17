package ch.ethz.iks.r_osgi.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import ch.ethz.iks.util.SmartSerializer;

/**
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 */
public class LeaseUpdateMessage extends RemoteOSGiMessage {

	public static final short TOPIC_UPDATE = 0;

	public static final short SERVICE_ADDED = 1;

	public static final short SERVICE_MODIFIED = 2;

	public static final short SERVICE_REMOVED = 3;

	/**
	 * 
	 */
	private short type;

	/**
	 * 
	 */
	private Object[] content;

	/**
	 * 
	 */
	private String serviceID;

	/**
	 * creates a new LeaseUpdateMessage for topic updates.
	 * 
	 * @param addedTopics
	 * @param removedTopics
	 */
	public LeaseUpdateMessage() {
		super(LEASE_UPDATE);
	}

	/**
	 * creates a new LeaseUpdateMessage from a network packet:
	 * 
	 * <pre>
	 *     0                   1                   2                   3
	 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *    |       R-OSGi header (function = InvokeMsg = 3)                |
	 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *    |   update type  |  length of &lt;url&gt;   |  &lt;url&gt; String  \
	 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *    |   service information or url or topic array                      \
	 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * </pre>.
	 * 
	 * @param input
	 *            an <code>ObjectInputStream</code> that provides the body of
	 *            a R-OSGi network packet.
	 * @throws IOException
	 *             in case of IO failures.
	 */
	LeaseUpdateMessage(final ObjectInputStream input) throws IOException {
		super(LEASE_UPDATE);
		type = input.readShort();
		serviceID = input.readUTF();
		content = (Object[]) SmartSerializer.deserialize(input);
	}

	/**
	 * write the body of the message to a stream.
	 * 
	 * @param out
	 *            the ObjectOutputStream.
	 * @throws IOException
	 *             in case of IO failures.
	 * @see ch.ethz.iks.r_osgi.messages.RemoteOSGiMessage#getBody()
	 */
	public void writeBody(final ObjectOutputStream out) throws IOException {
		out.writeShort(type);
		out.writeUTF(serviceID);
		SmartSerializer.serialize(content, out);
	}

	public short getType() {
		return type;
	}

	public void setType(short type) {
		this.type = type;
	}

	public String getServiceID() {
		return serviceID;
	}

	public void setServiceID(final String serviceID) {
		this.serviceID = serviceID;
	}

	public Object[] getPayload() {
		return content;
	}

	public void setPayload(final Object[] content) {
		this.content = content;
	}

	/**
	 * String representation for debug outputs.
	 * 
	 * @return a string representation.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[STATE_UPDATE] - XID: ");
		buffer.append(xid);
		buffer.append(", service ");
		buffer.append("#" + serviceID);
		buffer.append(", type ");
		buffer.append(type);
		if (type == TOPIC_UPDATE) {
			buffer.append(", topics added: ");
			buffer.append(Arrays.asList((String[]) content[0]));
			buffer.append(", topics removed: ");
			buffer.append(Arrays.asList((String[]) content[1]));
		} else {
			buffer.append(", service interfaces: ");
			buffer.append(Arrays.asList((String[]) content[0]));
			buffer.append(", properties: ");
			buffer.append(content[1]);
		}
		return buffer.toString();
	}

}
