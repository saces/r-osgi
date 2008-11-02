package ch.ethz.iks.r_osgi.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class DeliverBundlesMessage extends RemoteOSGiMessage {

	private byte[][] bytes;

	public DeliverBundlesMessage() {
		super(RemoteOSGiMessage.DELIVER_BUNDLES);
	}

	public DeliverBundlesMessage(final ObjectInputStream input)
			throws IOException {
		super(RemoteOSGiMessage.DELIVER_BUNDLES);
		final int bundleCount = input.readInt();
		bytes = new byte[bundleCount][];
		for (int i = 0; i < bundleCount; i++) {
			bytes[i] = readBytes(input);
		}
	}

	protected void writeBody(ObjectOutputStream output) throws IOException {
		output.writeInt(bytes.length);
		for (int i = 0; i < bytes.length; i++) {
			writeBytes(output, bytes[i]);
		}
	}

	public byte[][] getDependencies() {
		return bytes;
	}

	public void setDependencies(final byte[][] bytes) {
		this.bytes = bytes;
	}

	/**
	 * String representation for debug outputs.
	 * 
	 * @return a string representation.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("[DELIVER_BUNDLES]"); //$NON-NLS-1$
		buffer.append("- XID: "); //$NON-NLS-1$
		buffer.append(xid);
		buffer.append(", ... "); //$NON-NLS-1$
		return buffer.toString();
	}

}
