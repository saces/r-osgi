package ch.ethz.iks.r_osgi.transport.http;

import java.io.IOException;

import ch.ethz.iks.r_osgi.Remoting;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.channels.ChannelEndpoint;
import ch.ethz.iks.r_osgi.channels.NetworkChannel;
import ch.ethz.iks.r_osgi.channels.NetworkChannelFactory;

public class HTTPNetworkChannelFactory implements NetworkChannelFactory {

	static final String[] PROTOCOL_SCHEMES = { "http", "https" };

	public void activate(Remoting remoting) throws IOException {
		
	}

	public void deactivate(Remoting remoting) throws IOException {

	}

	public NetworkChannel getConnection(ChannelEndpoint endpoint,
			URI endpointURI) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public URI getListeningAddress(String protocol) {
		// TODO Auto-generated method stub
		return null;
	}

}
