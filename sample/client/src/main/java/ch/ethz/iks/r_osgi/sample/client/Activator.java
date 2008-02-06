package ch.ethz.iks.r_osgi.sample.client;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.RemoteServiceReference;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.sample.api.ServiceInterface;
import ch.ethz.iks.r_osgi.service_discovery.ServiceDiscoveryListener;

public class Activator implements BundleActivator {

	private ServiceReference sref;

	private RemoteOSGiService remote;

	private ServiceInterface service;

	private Thread clientThread;

	public void start(final BundleContext context) {
		try {
			System.out.println("starting sample client");

			sref = context.getServiceReference(RemoteOSGiService.class
					.getName());
			if (sref != null) {
				remote = (RemoteOSGiService) context.getService(sref);
			} else {
				throw new BundleException("OSGi remote service is not present.");
			}

			if (Boolean.getBoolean("ch.ethz.iks.r_osgi.service.discovery")) {
				context.registerService(ServiceDiscoveryListener.class
						.getName(), new ServiceDiscoveryListener() {

					public void announceService(String serviceInterface, URI uri) {
						remote.connect(uri);
						final RemoteServiceReference ref = remote
								.getRemoteServiceReference(uri);
						service = (ServiceInterface) remote
								.getRemoteService(ref);
						clientThread = new ClientThread();
						clientThread.start();
					}

					public void discardService(String serviceInterface, URI uri) {
						System.out.println("LOST SERVICE " + uri);

					}

				}, null);

			} else {
				final URI uri = new URI(System.getProperty(
						"ch.ethz.iks.r_osgi.service.uri",
						"r-osgi://localhost:9278"));
				// final URI uri = new URI("r-osgi://84.73.219.12:9278");
				// final URI uri = new URI("btspp://0010DCE96CB8:1");
				// final URI uri = new URI("btspp://0014A4D46D9A:1");
				// final URI uri = new URI("r-osgi://localhost:9270");
				remote.connect(uri);
				final RemoteServiceReference ref = remote
						.getRemoteServiceReferences(uri, ServiceInterface.class
								.getName(), null)[0];
				System.out.println("REFERENCE " + ref);
				service = (ServiceInterface) remote.getRemoteService(ref);
				clientThread = new ClientThread();
				clientThread.start();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stop(final BundleContext context) throws Exception {
		// if the thread exists, interrupt it
		if (clientThread != null) {
			clientThread.interrupt();
			clientThread = null;
		}
		remote = null;
	}

	private class ClientThread extends Thread {
		public void run() {
			setName("SampleClientThread");
			try {
				int i = 1;
				while (!isInterrupted()) {
					synchronized (this) {
						System.out.println("Invoking remote service:");
						System.out.println(service.echoService("my message",
								new Integer(i)));
						System.out
								.println(service.reverseService("my message"));
						System.out.println("calling local");
						try {
							service.local();
						} catch (RuntimeException r) {
							r.printStackTrace();
						}
						service.printRemote(i, 0.987654321F);
						System.out.println(service.equals(new Integer(10)));
						if (i <= 10) {
							i++;
						}
						service.verifyBlock("This is a test".getBytes(), 0, 1,
								2);
						wait(5000);
					}
				}
			} catch (InterruptedException ie) {
				// let the thread terminate
			}
		}
	};

}
