package ch.ethz.iks.r_osgi.sample.http.client;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import ch.ethz.iks.slp.ServiceURL;

import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.sample.api.ServiceInterface;

public class HttpTransportTest implements BundleActivator {

	private RemoteOSGiService remote;

	public void start(BundleContext context) throws Exception {

		// register event handler
		final Dictionary properties = new Hashtable();
		properties.put(EventConstants.EVENT_TOPIC,
				new String[] { "test/topic" });
		context.registerService(EventHandler.class.getName(),
				new EventHandler() {

					public void handleEvent(Event event) {
						System.out.println();
						System.out
								.println("-----------------------------------");
						System.out.println("RECEIVED EVENT BY HTTP: " + event);
						System.out
								.println("-----------------------------------");
						System.out.println();
					}

				}, properties);

		ServiceReference sref = context
				.getServiceReference(RemoteOSGiService.class.getName());
		if (sref == null) {
			throw new RuntimeException("No R-OSGi service found.");
		}
		remote = (RemoteOSGiService) context.getService(sref);

		System.out.println("TRYING TO ESTABLISH CONNECTION TO HOST");

		// ServiceURL[] services = remote.connect(InetAddress
		// .getByName("10.1.9.204"), 8080, "http");

		ServiceURL[] services = remote.connect(InetAddress
				.getByName("flowsgi.inf.ethz.ch"), 8080, "http");

		System.out.println("CONNECTED. AVAILABLE SERVICES ARE "
				+ Arrays.asList(services));

		// final ServiceURL url = new ServiceURL(
		// "service:osgi:ch/ethz/iks/r_osgi/sample/api/ServiceInterface://http://10.1.9.204:8080/9",
		// -1);

		final ServiceURL url = new ServiceURL(
				"service:osgi:ch/ethz/iks/r_osgi/sample/api/ServiceInterface://http://flowsgi.inf.ethz.ch:8080/9",
				-1);

		System.out.println("URL IS " + url);
		System.out.println("FETCHING ...");
		remote.fetchService(url);
		System.out.println("FETCHED ...");
		final ServiceInterface test = (ServiceInterface) remote
				.getFetchedService(services[0]);
		System.out.println("STARTING THREAD ...");
		new Thread() {
			public void run() {
				try {
					int i = 0;
					while (remote != null) {
						System.out.println();
						System.out.println();
						System.out.println("Invoking the remote service ...");
						System.out.println(test.echoService(
								"THIS IS TRANSMITTED BY HTTP !!!", new Integer(
										1 + (i++ % 5))));
						Thread.sleep(4000);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	public void stop(BundleContext context) throws Exception {
		remote = null;
	}

}
