package ch.ethz.iks.r_osgi.test;

import java.util.Arrays;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.RemoteServiceReference;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.sample.api.ServiceInterface;
import ch.ethz.iks.r_osgi.test.service.impl.TestCaseWithNestedFramework;

public class ChannelReconnectionTest extends TestCaseWithNestedFramework {

	private RemoteOSGiService remote;
	private Bundle rosgiBundle;

	protected void setUp() throws Exception {
		super.setUp();
		context = Activator.getActivator().getContext();
		remote = Activator.getActivator().getR_OSGi();
		rosgiBundle = Activator.getActivator().getR_OSGiBundle();

	}

	protected void tearDown() throws Exception {
		super.tearDown();

		context = null;
		remote = null;
	}

	public void testSimpleReconnect() throws Exception {
		final ServiceReference ref = context
				.getServiceReference(ServiceInterface.class.getName());
		assertNotNull(ref);
		final String serviceID = ref.getProperty(Constants.SERVICE_ID)
				.toString();

		final EmbeddedROSGiFramework fw1 = newROSGiFramework(new String[] { "ch.ethz.iks.r_osgi.sample.dependency" });

		final RemoteOSGiService r1 = fw1.getRemoteOSGiService();

		assertNotSame(remote, r1);
		assertTrue(remote.getListeningPort("r-osgi") != fw1.getListeningPort());

		final RemoteServiceReference[] rrefs = r1.connect(new URI(
				"r-osgi://localhost:9278"));
		assertNotNull(rrefs);
		System.err.println(Arrays.toString(rrefs));
		assertEquals(1, rrefs.length);

		final ServiceInterface service = (ServiceInterface) r1
				.getRemoteService(rrefs[0]);

		assertTrue(bundleExists(fw1.getBundleContext(),
				"proxy for r-osgi://localhost:9278#" + serviceID));

		rosgiBundle.stop();

		assertFalse(bundleExists(fw1.getBundleContext(),
				"proxy for r-osgi://localhost:9278#" + serviceID));

		rosgiBundle.start();

		final RemoteServiceReference[] rrefs2 = r1.connect(new URI(
				"r-osgi://localhost:9278"));
	}
}
