package ch.ethz.iks.r_osgi.impl;

import java.io.IOException;
import java.io.OutputStream;


public class OutputStreamProxy extends OutputStream {

	private short streamID;
	
	private ChannelEndpointImpl endpoint;
	
	public OutputStreamProxy(final short streamID, final ChannelEndpointImpl endpoint) {
		this.streamID = streamID;
		this.endpoint = endpoint;
	}
	
	public void write(int b) throws IOException {
		endpoint.writeStream(streamID, b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		endpoint.writeStream(streamID, b, off, len);
	}
	
}
