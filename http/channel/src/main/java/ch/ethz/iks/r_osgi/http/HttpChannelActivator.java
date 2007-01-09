package ch.ethz.iks.r_osgi.http;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import ch.ethz.iks.r_osgi.NetworkChannelFactory;

/**
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 */
public class HttpChannelActivator implements BundleActivator {

	/**
	 * 
	 */
	private ServiceRegistration reg;
	
	/**
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		Hashtable properties = new Hashtable();
		properties.put(NetworkChannelFactory.PROTOCOL_PROPERTY,
				HttpChannelFactory.PROTOCOL);
		reg = context.registerService(NetworkChannelFactory.class.getName(),
				new HttpChannelFactory(), properties);
	}

	/**
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		reg.unregister();
	}

}
