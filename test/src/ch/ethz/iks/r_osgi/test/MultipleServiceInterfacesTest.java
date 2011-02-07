package ch.ethz.iks.r_osgi.test;

import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.RemoteServiceReference;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.test.service.ServiceInterfaceOne;
import ch.ethz.iks.r_osgi.test.service.ServiceInterfaceThree;
import ch.ethz.iks.r_osgi.test.service.ServiceInterfaceTwo;
import ch.ethz.iks.r_osgi.test.service.impl.MultipleInterfaceServiceImpl;

public class MultipleServiceInterfacesTest extends TestCase {

	private static final URI uri = new URI("r-osgi://localhost:9278");

	private RemoteOSGiService remote;

	private BundleContext context;

	private Object testService;

	private ServiceRegistration reg;

	public MultipleServiceInterfacesTest() {
		super("MultipleServiceInterfacesTest");
	}

	protected void setUp() throws Exception {
		super.setUp();
		context = Activator.getActivator().getContext();
		remote = Activator.getActivator().getR_OSGi();
		final Dictionary props = new Hashtable();
		props.put(RemoteOSGiService.R_OSGi_REGISTRATION, Boolean.TRUE);
		reg = context.registerService(
				new String[] { ServiceInterfaceOne.class.getName(),
						ServiceInterfaceTwo.class.getName(),
						ServiceInterfaceThree.class.getName() },
				new MultipleInterfaceServiceImpl(), props);

		remote.connect(uri);
		// potential race condition, ServiceTracker versus
		// getRemoteServiceReferences
		// adding some delay
		Thread.sleep(100);
		final RemoteServiceReference[] refs = remote
				.getRemoteServiceReferences(uri,
						ServiceInterfaceOne.class.getName(), null);
		assertNotNull(refs);
		assertTrue(refs.length > 0);

		testService = remote.getRemoteService(refs[0]);

	}

	protected void tearDown() throws Exception {
		remote.disconnect(uri);
		reg.unregister();
		super.tearDown();
	}

	public void testInterfaces() {
		assertEquals(((ServiceInterfaceOne) testService).callOne("dummy"),
				"ONE");
		assertEquals(((ServiceInterfaceTwo) testService).callTwo("dummy"),
				"TWO");
		assertEquals(((ServiceInterfaceThree) testService).callThree("dummy"),
				"THREE");
	}
}
