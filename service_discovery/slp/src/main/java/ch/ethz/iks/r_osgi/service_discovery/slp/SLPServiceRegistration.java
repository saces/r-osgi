package ch.ethz.iks.r_osgi.service_discovery.slp;

import java.util.Dictionary;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.slp.Advertiser;
import ch.ethz.iks.slp.ServiceLocationException;
import ch.ethz.iks.slp.ServiceURL;

public class SLPServiceRegistration {

	private ServiceURL[] urls;

	private Dictionary properties;

	SLPServiceRegistration(final ServiceReference ref,
			final Dictionary properties, final URI uri) {
		this.properties = properties;
		final String[] interfaces = (String[]) ref
				.getProperty(Constants.OBJECTCLASS);
		urls = new ServiceURL[interfaces.length];
		for (int i = 0; i < interfaces.length; i++) {
			try {
				urls[i] = new ServiceURL("service:osgi:"
						+ interfaces[i].replace('.', '/') + "://"
						+ uri.getScheme() + "://" + uri.getHostName() + ":"
						+ uri.getPort() + "/" + uri.getFragment(),
						SLPServiceDiscoveryHandler.DEFAULT_SLP_LIFETIME * 1000);
			} catch (ServiceLocationException sle) {
				sle.printStackTrace();
			}
		}
	}

	void register(final Advertiser advertiser) throws ServiceLocationException {
		// TODO: remove debug output
		System.out.println("registering " + java.util.Arrays.asList(urls));
		for (int i = 0; i < urls.length; i++) {
			advertiser.register(urls[i], properties);
		}
	}

	public void unregister(Advertiser advertiser)
			throws ServiceLocationException {
		// TODO: remove debug output
		System.out.println("unregistering " + java.util.Arrays.asList(urls));
		for (int i = 0; i < urls.length; i++) {
			advertiser.deregister(urls[i]);
		}
	}
}
