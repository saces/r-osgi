package ch.ethz.iks.r_osgi.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Dictionary;

import ch.ethz.iks.slp.ServiceLocationException;
import ch.ethz.iks.slp.ServiceURL;
import ch.ethz.iks.util.SmartSerializer;

public class StateUpdateMessage extends RemoteOSGiMessageImpl {

	/**
	 * the serviceURL string of the remote service.
	 */
	private String serviceURL;

	/**
	 * 
	 */
	private short state;

	/**
	 * the signature of the method that is requested to be invoked.
	 */
	private Dictionary attributes;

	/**
	 * creates a new InvokeMethodMessage.
	 * 
	 * @param service
	 *            the serviceURL of the service.
	 * @param methodSignature
	 *            the method signature.
	 * @param params
	 *            the parameter that are passed to the method.
	 */
	StateUpdateMessage(final String service, final short newState,
			final Dictionary newAttributes) {
		funcID = STATE_UPDATE;
		this.serviceURL = service;
		this.state = newState;
		this.attributes = newAttributes;
	}

	/**
	 * creates a new InvokeMethodMessage from network packet:
	 * 
	 * <pre>
	 *    0                   1                   2                   3
	 *    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *   |       R-OSGi header (function = InvokeMsg = 3)                |
	 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *   |   length of &lt;ServiceURL&gt;     |    &lt;ServiceURL&gt; String       \
	 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *   |    length of &lt;MethodSignature&gt;     |     &lt;MethodSignature&gt; String       \
	 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *   |   number of param blocks      |     Param blocks (if any)     \
	 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * </pre>.
	 * 
	 * @param input
	 *            an <code>ObjectInputStream</code> that provides the body of
	 *            a R-OSGi network packet.
	 * @throws IOException
	 *             in case of IO failures.
	 */
	StateUpdateMessage(final ObjectInputStream input) throws IOException {
		funcID = STATE_UPDATE;
		serviceURL = input.readUTF();
		state = input.readShort();
		attributes = (Dictionary) SmartSerializer.deserialize(input);
	}

	/**
	 * write the body of the message to a stream.
	 * 
	 * @param out
	 *            the ObjectOutputStream.
	 * @throws IOException
	 *             in case of IO failures.
	 * @see ch.ethz.iks.r_osgi.impl.RemoteOSGiMessageImpl#getBody()
	 */
	public void writeBody(final ObjectOutputStream out) throws IOException {
		out.writeUTF(serviceURL);
		out.writeShort(state);
		SmartSerializer.serialize(attributes, out);
	}

	/**
	 * get the service url of the service.
	 * 
	 * @return the service url as string.
	 */
	String getServiceURL() {
		return serviceURL;
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
			throws IllegalArgumentException {
		try {
			final ServiceURL original = new ServiceURL(serviceURL, 0);
			final ServiceURL restamped = new ServiceURL(original
					.getServiceType()
					+ "://"
					+ (protocol != null ? (protocol + "://") : "")
					+ host + ":" + port + original.getURLPath(), 0);
			serviceURL = restamped.toString();
		} catch (ServiceLocationException sle) {
			throw new IllegalArgumentException(sle.getMessage());
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
		buffer.append("[STATE_UPDATE] - XID: ");
		buffer.append(xid);
		buffer.append(", serviceURL ");
		buffer.append(serviceURL);
		buffer.append(", state ");
		buffer.append(state);
		buffer.append(", attributes ");
		buffer.append(attributes);
		return buffer.toString();
	}

}
