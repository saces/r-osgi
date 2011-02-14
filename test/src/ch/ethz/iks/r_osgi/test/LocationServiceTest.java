package ch.ethz.iks.r_osgi.test;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.ServiceRegistration;

import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.RemoteServiceReference;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.test.service.ILocationService;
import ch.ethz.iks.r_osgi.test.service.impl.LocationService;
import ch.ethz.iks.r_osgi.test.service.impl.TestCaseWithNestedFramework;

public class LocationServiceTest extends TestCaseWithNestedFramework {

	private RemoteOSGiService remote;
	private ServiceRegistration reg;

	protected void setUp() throws Exception {
		super.setUp();

		context = Activator.getActivator().getContext();
		remote = Activator.getActivator().getR_OSGi();
		final Dictionary props = new Hashtable();
		props.put(RemoteOSGiService.R_OSGi_REGISTRATION, Boolean.TRUE);
		reg = context.registerService(ILocationService.class.getName(),
				new LocationService(), props);
	}

	protected void tearDown() throws Exception {
		super.tearDown();

		reg.unregister();
		context = null;
		remote = null;
	}

	public void testLocationService() throws Exception {
		final EmbeddedROSGiFramework fw1 = newROSGiFramework(new String[] {});
		final RemoteOSGiService fw1rem = fw1.getRemoteOSGiService();

		assertNotNull(fw1rem);

		final URI uri = URI.create("r-osgi://localhost:9278");
		final RemoteServiceReference[] rrefs = fw1rem.connect(uri);

		assertNotNull(rrefs);

		RemoteServiceReference rref = null;
		for (int i = 0; i < rrefs.length; i++) {
			if (Arrays.asList(rrefs[i].getServiceInterfaces()).contains(
					ILocationService.class.getName())) {
				rref = rrefs[i];
				break;
			}
		}

		assertNotNull(rref);
		System.out.println(rref);

		final Object service = fw1rem.getRemoteService(rref);

		System.out.println(service);

		assertNotNull(service);

		((ILocationService) service).Test(null);

		fw1.shutdown();
	}

}
