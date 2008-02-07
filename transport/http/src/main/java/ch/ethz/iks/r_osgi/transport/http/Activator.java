package ch.ethz.iks.r_osgi.transport.http;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import ch.ethz.iks.r_osgi.channels.NetworkChannelFactory;

public class Activator implements BundleActivator {

	public void start(final BundleContext context) throws Exception {
		final Dictionary properties = new Hashtable();
		properties.put(NetworkChannelFactory.PROTOCOL_PROPERTY,
				HTTPNetworkChannelFactory.PROTOCOL_SCHEMES);
		context.registerService(NetworkChannelFactory.class.getName(),
				new HTTPNetworkChannelFactory(), properties);
	}

	public void stop(final BundleContext context) throws Exception {

	}

}
