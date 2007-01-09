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
				.getByName("blumfeld"), 80, "http");

		System.out.println("CONNECTED. AVAILABLE SERVICES ARE "
				+ Arrays.asList(services));

		remote.fetchService(services[0]);
		final ServiceInterface test = (ServiceInterface) remote
				.getFetchedService(services[0]);
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println(test.echoService("THIS IS TRANSMITTED BY HTTP !!!",
				new Integer(1)));
	}

	public void stop(BundleContext context) throws Exception {
		remote = null;
	}

}
