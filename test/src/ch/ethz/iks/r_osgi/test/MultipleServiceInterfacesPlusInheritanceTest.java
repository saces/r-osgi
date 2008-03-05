package ch.ethz.iks.r_osgi.test;

import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.osgi.framework.BundleContext;

import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.RemoteServiceReference;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.test.service.ServiceInterfaceA;
import ch.ethz.iks.r_osgi.test.service.ServiceInterfaceB;
import ch.ethz.iks.r_osgi.test.service.ServiceInterfaceC;
import ch.ethz.iks.r_osgi.test.service.impl.MultipleInterfacesPlusInheritanceServiceImpl;

public class MultipleServiceInterfacesPlusInheritanceTest extends TestCase {

	private static final URI uri = new URI("r-osgi://localhost:9278");

	private RemoteOSGiService remote;

	private BundleContext context;

	private Object testService;

	public MultipleServiceInterfacesPlusInheritanceTest() {
		super("MultipleServiceInterfacesPlusInheritanceTest");
	}

	protected void setUp() throws Exception {
		super.setUp();
		context = Activator.getActivator().getContext();
		remote = Activator.getActivator().getR_OSGi();
		final Dictionary props = new Hashtable();
		props.put(RemoteOSGiService.R_OSGi_REGISTRATION, Boolean.TRUE);
		context.registerService(new String[] {
				ServiceInterfaceA.class.getName(),
				ServiceInterfaceB.class.getName(),
				ServiceInterfaceC.class.getName() },
				new MultipleInterfacesPlusInheritanceServiceImpl(), props);

		remote.connect(uri);
		final RemoteServiceReference[] refs = remote
				.getRemoteServiceReferences(uri, ServiceInterfaceA.class
						.getName(), null);
		assertNotNull(refs);
		assertTrue(refs.length > 0);

		testService = remote.getRemoteService(refs[0]);

	}

	protected void tearDown() throws Exception {
		remote.disconnect(uri);
		super.tearDown();
	}

	public void testInterfaces() {
		assertEquals(((ServiceInterfaceA) testService).fooA(), "A");
		assertEquals(((ServiceInterfaceB) testService).fooA(), "A");
		assertEquals(((ServiceInterfaceB) testService).fooB(), "B");
		assertEquals(((ServiceInterfaceC) testService).fooA(), "A");
		assertEquals(((ServiceInterfaceC) testService).fooB(), "B");
		assertEquals(((ServiceInterfaceC) testService).fooA(), "A");
	}
}
