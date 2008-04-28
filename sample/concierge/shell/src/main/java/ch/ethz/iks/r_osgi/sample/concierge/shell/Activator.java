package ch.ethz.iks.r_osgi.sample.concierge.shell;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

import ch.ethz.iks.concierge.shell.commands.ShellCommandGroup;
import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.RemoteServiceListener;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		final ServiceReference ref = context
				.getServiceReference(RemoteOSGiService.class.getName());
		if (ref == null) {
			throw new BundleException("R-OSGi is not running");
		}
		final RemoteOSGiService remote = (RemoteOSGiService) context
				.getService(ref);
		context.registerService(new String[] {
				ShellCommandGroup.class.getName(),
				RemoteServiceListener.class.getName() },
				new ShellPlugin(remote), null);
	}

	public void stop(BundleContext context) throws Exception {

	}

}
