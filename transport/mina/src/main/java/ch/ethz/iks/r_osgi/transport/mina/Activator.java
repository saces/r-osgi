package ch.ethz.iks.r_osgi.transport.mina;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import ch.ethz.iks.r_osgi.channels.NetworkChannelFactory;

public class Activator implements BundleActivator {

	public void start(final BundleContext context) throws Exception {
		final Dictionary properties = new Hashtable();
		properties.put(NetworkChannelFactory.PROTOCOL_PROPERTY,
				MinaNetworkChannelFactory.PROTOCOL_SCHEME);
		context.registerService(NetworkChannelFactory.class.getName(),
				new MinaNetworkChannelFactory(), properties);
	}

	public void stop(final BundleContext context) throws Exception {

	}

}
