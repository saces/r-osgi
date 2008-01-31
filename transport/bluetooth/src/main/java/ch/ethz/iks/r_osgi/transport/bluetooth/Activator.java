package ch.ethz.iks.r_osgi.transport.bluetooth;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import ch.ethz.iks.r_osgi.channels.NetworkChannelFactory;
import ch.ethz.iks.r_osgi.service_discovery.ServiceDiscoveryHandler;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		Dictionary properties = new Hashtable();
		properties.put(NetworkChannelFactory.PROTOCOL_PROPERTY,
				BluetoothNetworkChannelFactory.PROTOCOL);
		context.registerService(new String[] {
				NetworkChannelFactory.class.getName(),
				ServiceDiscoveryHandler.class.getName() },
				new BluetoothNetworkChannelFactory(), properties);
	}

	public void stop(BundleContext context) throws Exception {

	}

}
