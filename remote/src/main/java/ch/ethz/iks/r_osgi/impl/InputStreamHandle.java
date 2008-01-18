package ch.ethz.iks.r_osgi.impl;

import java.io.Serializable;

public class InputStreamHandle implements Serializable {

	private static final long serialVersionUID = 3774937077649735910L;

	private final short streamID;
	
	public InputStreamHandle(final short streamID) {
		this.streamID = streamID;
	}

	public short getStreamID() {
		return streamID;
	}
	
}
