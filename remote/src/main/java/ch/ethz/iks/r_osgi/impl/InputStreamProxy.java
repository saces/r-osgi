package ch.ethz.iks.r_osgi.impl;

import java.io.IOException;
import java.io.InputStream;


public class InputStreamProxy extends InputStream {

	private short streamID;
	
	private ChannelEndpointImpl endpoint;
	
	public InputStreamProxy(final short streamID, final ChannelEndpointImpl endpoint) {
		this.streamID = streamID;
		this.endpoint = endpoint;
	}
	
	public int read() throws IOException {
		return endpoint.readStream(streamID);
	}

	public int read(byte[] b, int off, int len) throws IOException {
		return endpoint.readStream(streamID, b, off, len);
	}
	
}
