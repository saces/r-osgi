package ch.ethz.iks.r_osgi.sample.service;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import ch.ethz.iks.concierge.shell.commands.ShellCommandGroup;
import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.SurrogateRegistration;
import ch.ethz.iks.r_osgi.sample.api.ServiceInterface;

public class Activator implements BundleActivator, SurrogateRegistration {

	private ServiceRegistration registration;

	public void start(BundleContext context) throws Exception {

		// register the sample service and enable R-OSGi remote access by
		// building a proxy on the client side
		final Hashtable properties = new Hashtable();
		properties.put(RemoteOSGiService.R_OSGi_REGISTRATION, Boolean.TRUE);
		properties.put(RemoteOSGiService.SMART_PROXY,
				SmartService.class.getName());

		// properties.put(RemoteOSGiService.R_OSGi_REGISTRATION,
		// RemoteOSGiService.TRANSFER_BUNDLE_POLICY);

		final ServiceRegistration reg = context.registerService(
				ServiceInterface.class.getName(), new ServiceImpl(), null);

		properties.put(SurrogateRegistration.SERVICE_REFERENCE,
				reg.getReference());

		registration = context.registerService(
				SurrogateRegistration.class.getName(), this, properties);

		final ServiceReference ref = context
				.getServiceReference(EventAdmin.class.getName());
		if (ref != null) {
			final EventAdmin eventAdmin = (EventAdmin) context.getService(ref);
			new Thread() {
				public void run() {
					setName("SampleServiceEventThread");
					final Event event = new Event("test/topic",
							(Dictionary) null);
					while (true) {
						System.out.println();
						System.out.println("SENDING EVENT " + event);
						eventAdmin.postEvent(event);
						try {
							Thread.sleep(30000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			};
			// }.start();

			// properties.clear();

			context.registerService(ShellCommandGroup.class.getName(),
					new ShellCommandGroup() {
						public String getGroup() {
							return "testservice";
						}

						public String getHelp() {
							return "testservice has only one command: \"set\" to set a variable service property";
						}

						public void handleCommand(String command, String[] args)
								throws Exception {
							if ("set".equals("set")) {
								if (args.length == 1) {
									properties.put("variable", args[0]);
									reg.setProperties(properties);
									return;
								}
							}
							System.err.println("Unknown command");
						}
					}, new Hashtable());

			new Thread() {
				public void run() {
					try {
						System.out
								.println("PRESS ANY KEY TO CAUSE A PROPERTY UPDATE");
						System.in.read();
						Dictionary newProps = new Hashtable();
						newProps.put("Dummy", "value");
						reg.setProperties(newProps);
					} catch (final IOException ioe) {
						ioe.printStackTrace();
					}
				}
			}.start();
		} else {
			System.err.println();
			System.err
					.println("NO EVENT ADMIN FOUND. CANNOT SEND REMOTE EVENTS.");
			System.err.println();
		}
	}

	public void stop(BundleContext context) throws Exception {
		registration.unregister();
	}

}
