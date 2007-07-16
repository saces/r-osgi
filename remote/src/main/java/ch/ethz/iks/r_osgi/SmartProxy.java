package ch.ethz.iks.r_osgi;

import org.osgi.framework.BundleContext;

/**
 * Interface for smart proxies.
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 * @since 0.6.4
 */
public interface SmartProxy {

	/**
	 * This method is called when the smart proxy is started. It can be used to
	 * retrieve other services.
	 * 
	 * @param context
	 *            the bundle context of the proxy bundle.
	 */
	public void started(final BundleContext context);

	/**
	 * This method is called when the smart proxy is stopped.
	 * 
	 * @param context
	 *            the bundle context.
	 */
	public void stopped(final BundleContext context);

}
