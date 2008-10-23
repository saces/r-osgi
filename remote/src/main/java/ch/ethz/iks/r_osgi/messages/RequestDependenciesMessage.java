package ch.ethz.iks.r_osgi.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

public class RequestDependenciesMessage extends RemoteOSGiMessage {

	private String[] packages;

	public RequestDependenciesMessage() {
		super(RemoteOSGiMessage.REQUEST_DEPENDENCIES);
	}

	public RequestDependenciesMessage(ObjectInputStream input)
			throws IOException {
		super(RemoteOSGiMessage.REQUEST_DEPENDENCIES);
		packages = readStringArray(input);
	}

	public void writeBody(ObjectOutputStream output) throws IOException {
		writeStringArray(output, packages);
	}

	public String[] getPackages() {
		return packages;
	}

	public void setPackages(final String[] packages) {
		this.packages = packages;
	}

	/**
	 * String representation for debug outputs.
	 * 
	 * @return a string representation.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("[REQUEST_DEPENDENCIES]"); //$NON-NLS-1$
		buffer.append("- XID: "); //$NON-NLS-1$
		buffer.append(xid);
		buffer.append(", packages: "); //$NON-NLS-1$
		buffer.append(Arrays.asList(packages));
		return buffer.toString();
	}

}
