package ch.ethz.iks.r_osgi.sample.client;

import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import ch.ethz.iks.r_osgi.RemoteServiceEvent;
import ch.ethz.iks.r_osgi.RemoteServiceListener;
import ch.ethz.iks.r_osgi.RemoteOSGiException;
import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.sample.api.ServiceInterface;

public class Activator implements BundleActivator {

	private ServiceReference sref;

	private RemoteOSGiService remote;

	private RemoteServiceListener listener;

	private ServiceInterface service;

	private Thread usingThread;

	private boolean running = true;

	public void start(final BundleContext context) throws Exception {
		System.out.println("starting sample client");

		sref = context.getServiceReference(RemoteOSGiService.class.getName());
		if (sref != null) {
			remote = (RemoteOSGiService) context.getService(sref);
		} else {
			throw new BundleException("OSGi remote service is not present.");
		}

		listener = new RemoteServiceListener() {
			public void remoteServiceEvent(RemoteServiceEvent event) {
				switch (event.getType()) {
				case RemoteServiceEvent.REGISTERED:
					System.out.println("found service "
							+ event.getRemoteReference().getURI());
					try {
						// fetch the service
						remote.fetchService(event.getRemoteReference());
						service = (ServiceInterface) remote
								.getFetchedService(event.getRemoteReference());

						// and create a thread that makes use of the service
						usingThread = new Thread() {
							public void run() {
								setName("SampleClientThread");
								try {
									int i = 1;
									while (running) {
										synchronized (this) {
											System.out
													.println("Invoking remote service:");
											System.out.println(service
													.echoService("my message",
															new Integer(i)));
											System.out
													.println(service
															.reverseService("my message"));
											System.out.println("calling local");
											try {
												service.local();
											} catch (RuntimeException r) {
												r.printStackTrace();
											}
											service
													.printRemote(i,
															0.987654321F);
											System.out.println(service
													.equals(new Integer(10)));
											if (i <= 10) {
												i++;
											}
											wait(5000);
										}
									}
								} catch (InterruptedException ie) {
									// let the thread terminate
								}
							}
						};
						usingThread.start();

					} catch (RemoteOSGiException e) {
						e.printStackTrace();
					}

					return;
				case RemoteServiceEvent.UNREGISTERING:
					System.out.println("lost service "
							+ event.getRemoteReference().getURI());
					usingThread.interrupt();
					return;
				}

			}
		};
		final Dictionary props = new Hashtable();
		props.put(RemoteServiceListener.SERVICE_INTERFACES,
				new String[] { ServiceInterface.class.getName() });
		context.registerService(RemoteServiceListener.class.getName(),
				listener, props);

		final Dictionary properties = new Hashtable();
		properties.put(EventConstants.EVENT_TOPIC,
				new String[] { "test/topic" });
		context.registerService(EventHandler.class.getName(),
				new EventHandler() {

					public void handleEvent(Event arg0) {
						System.out.println("RECEIVED " + arg0);
					}

				}, properties);
	}

	public void stop(final BundleContext context) throws Exception {
		running = false;

		// if the thread exists, interrupt it
		if (usingThread != null) {
			usingThread.interrupt();
			usingThread = null;
		}
		// cleanup
		remote = null;
		context.ungetService(sref);
	}

}
