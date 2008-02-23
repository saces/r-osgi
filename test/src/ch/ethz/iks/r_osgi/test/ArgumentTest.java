package ch.ethz.iks.r_osgi.test;

import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.osgi.framework.BundleContext;

import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.RemoteServiceReference;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.test.service.ArgumentTestService;
import ch.ethz.iks.r_osgi.test.service.impl.ArgumentTestServiceImpl;

public class ArgumentTest extends TestCase {

	private static final URI uri = new URI("r-osgi://localhost:9278");

	private RemoteOSGiService remote;

	private BundleContext context;

	private ArgumentTestService testService;

	public ArgumentTest() {
		super("ArgumentTest");
	}

	protected void setUp() throws Exception {
		super.setUp();
		context = Activator.getActivator().getContext();
		remote = Activator.getActivator().getR_OSGi();
		final Dictionary props = new Hashtable();
		props.put(RemoteOSGiService.R_OSGi_REGISTRATION, Boolean.TRUE);
		context.registerService(ArgumentTestService.class.getName(),
				new ArgumentTestServiceImpl(), props);

		remote.connect(uri);
		final RemoteServiceReference[] refs = remote
				.getRemoteServiceReferences(uri, ArgumentTestService.class
						.getName(), null);
		assertNotNull(refs);
		assertTrue(refs.length > 0);

		testService = (ArgumentTestService) remote.getRemoteService(refs[0]);
	}

	protected void tearDown() throws Exception {
		remote.disconnect(uri);
		super.tearDown();
	}

	public void testArguments() {
		testService.byte0((byte) 1);
		testService.byte1("This is a test".getBytes());
		testService.byte3(new byte[][][] {
				{ "ABC".getBytes(), "DEF".getBytes() },
				{ "GHI".getBytes(), "This is a test".getBytes() } });
		final Byte b = new Byte("100");
		testService.byteObj0(b);
		testService.byteObj1(b);
		testService.intObj1(1000);
	}
}
