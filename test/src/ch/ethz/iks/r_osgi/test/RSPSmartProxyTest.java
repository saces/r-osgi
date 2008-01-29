package ch.ethz.iks.r_osgi.test;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import ch.ethz.iks.r_osgi.RemoteOSGiService;

import junit.framework.TestCase;

public class RSPSmartProxyTest extends TestCase {

	private BundleContext context;
	private RemoteOSGiService remoteService;

	private Bundle api_bundle;
	private Bundle extra_bundle;
	private Bundle master_bundle;
	private Bundle slave_bundle;

	public void setUp() throws BundleException, IOException {
		context = Activator.getActivator().getContext();
		remoteService = Activator.getActivator().getR_OSGi();

		URL url = context.getBundle().getEntry(
				"/resources/RSPTestSlave_api_1.0.0.jar");
		assertNotNull(url);
		api_bundle = context.installBundle(url.toString(), url.openStream());
		api_bundle.start();

		url = context.getBundle().getEntry("/resources/RSPTestExtra_1.0.0.jar");
		assertNotNull(url);
		extra_bundle = context.installBundle(url.toString(), url.openStream());
		extra_bundle.start();

		url = context.getBundle().getEntry("/resources/RSPTestSlave_1.0.0.jar");
		assertNotNull(url);
		slave_bundle = context.installBundle(url.toString(), url.openStream());

		url = context.getBundle()
				.getEntry("/resources/RSPTestMaster_1.0.0.jar");
		assertNotNull(url);
		master_bundle = context.installBundle(url.toString(), url.openStream());
	}

	public void tearDown() throws BundleException {
		api_bundle.uninstall();
		extra_bundle.uninstall();
		master_bundle.uninstall();
		slave_bundle.uninstall();
	}

	public void testSmartProxy() throws Exception {
		try {
			slave_bundle.start();
			Thread.sleep(1000);
			master_bundle.start();
		} catch (Throwable t) {
			t.printStackTrace();
			fail(t.getMessage());
		}
		Thread.sleep(1000);
	}

}
