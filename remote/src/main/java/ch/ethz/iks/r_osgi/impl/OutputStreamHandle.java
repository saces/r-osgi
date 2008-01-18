package ch.ethz.iks.r_osgi.impl;

import java.io.Serializable;

public class OutputStreamHandle implements Serializable {

	private static final long serialVersionUID = 5808802863290529323L;

	private final short streamID;
	
	public OutputStreamHandle(final short streamID) {
		this.streamID = streamID;
	}

	public short getStreamID() {
		return streamID;
	}
	
}
