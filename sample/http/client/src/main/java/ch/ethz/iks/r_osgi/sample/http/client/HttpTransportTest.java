package ch.ethz.iks.r_osgi.sample.http.client;

import java.net.InetAddress;
import java.util.Arrays;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import ch.ethz.iks.slp.ServiceURL;

import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.sample.api.ServiceInterface;

public class HttpTransportTest implements BundleActivator {

	private RemoteOSGiService remote;

	public void start(BundleContext context) throws Exception {
		ServiceReference sref = context
				.getServiceReference(RemoteOSGiService.class.getName());
		if (sref == null) {
			throw new RuntimeException("No R-OSGi service found.");
		}
		remote = (RemoteOSGiService) context.getService(sref);

		System.out.println("TRYING TO ESTABLISH CONNECTION TO HOST");

		ServiceURL[] services = remote.connect(InetAddress
				.getByName("10.1.9.204"), 8080, "http");

		// ServiceURL[] services = remote.connect(InetAddress
		// .getByName("192.168.24.1"), 8080, "http");

		System.out.println("CONNECTED. AVAILABLE SERVICES ARE "
				+ Arrays.asList(services));

		final ServiceURL url = new ServiceURL(
				"service:osgi:ch/ethz/iks/r_osgi/sample/api/ServiceInterface://http://10.1.9.204:8080/9",
				-1);
		
		System.out.println("URL IS " + url);
		System.out.println("URL PROTOCOL " + url.getProtocol());
		System.out.println("FETCHING ...");
		remote.fetchService(url);
		System.out.println("FETCHED ...");
		final ServiceInterface test = (ServiceInterface) remote
				.getFetchedService(services[0]);
		new Thread() {
			public void run() {
				System.out.println();
				System.out.println();
				System.out.println();
				System.out.println(test.echoService(
						"THIS IS TRANSMITTED BY HTTP !!!", new Integer(1)));
			}
		}.start();
	}

	public void stop(BundleContext context) throws Exception {
		remote = null;
	}

}
