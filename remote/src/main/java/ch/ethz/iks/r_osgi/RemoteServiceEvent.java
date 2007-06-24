package ch.ethz.iks.r_osgi;

import java.util.EventObject;

public final class RemoteServiceEvent extends EventObject {

	/**
	 * Type of service lifecycle change.
	 */
	private final transient int type;

	public static final int REGISTERED = 0x00000001;

	public static final int MODIFIED = 0x00000002;

	public static final int UNREGISTERING = 0x00000004;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RemoteServiceEvent(final int type,
			final RemoteServiceReference remoteRef) {
		super(remoteRef);
		this.type = type;
	}

	public final int getType() {
		return (type);
	}

	public RemoteServiceReference getRemoteReference() {
		return (RemoteServiceReference) this.getSource();
	}

}
