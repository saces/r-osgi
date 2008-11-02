package ch.ethz.iks.r_osgi.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class RequestBundleMessage extends RemoteOSGiMessage {

	private String serviceID;

	public RequestBundleMessage() {
		super(REQUEST_BUNDLE);

	}

	public RequestBundleMessage(final ObjectInputStream input)
			throws IOException {
		super(REQUEST_BUNDLE);
		serviceID = input.readUTF();
	}

	protected void writeBody(final ObjectOutputStream out) throws IOException {
		out.writeUTF(serviceID);
	}

	/**
	 * get the service ID.
	 * 
	 * @return the service ID.
	 */
	public String getServiceID() {
		return serviceID;
	}

	/**
	 * get the service ID.
	 * 
	 * @param serviceID
	 *            the service ID.
	 */
	public void setServiceID(final String serviceID) {
		this.serviceID = serviceID;
	}

	/**
	 * String representation for debug outputs.
	 * 
	 * @return a string representation.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("[REQUEST_BUNDLE]"); //$NON-NLS-1$
		buffer.append("- XID: "); //$NON-NLS-1$
		buffer.append(xid);
		buffer.append(", serviceID: "); //$NON-NLS-1$
		buffer.append(serviceID);
		return buffer.toString();
	}

}
