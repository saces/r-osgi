package ch.ethz.iks.r_osgi.test;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.sample.api.ServiceInterface;
import ch.ethz.iks.r_osgi.service_discovery.ServiceDiscoveryListener;
import ch.ethz.iks.r_osgi.test.service.impl.Service;
import junit.framework.TestCase;

public class JSLPServiceDiscoveryTestDisabled extends TestCase {

	private ArrayList queue = new ArrayList();

	private BundleContext context;

	public JSLPServiceDiscoveryTestDisabled() {
		context = Activator.getActivator().getContext();

	}

	public void testServiceDiscovery() throws InterruptedException {
		// register the service
		final Dictionary props = new Hashtable();
		props.put(RemoteOSGiService.R_OSGi_REGISTRATION, Boolean.TRUE);

		final ServiceRegistration serviceReg = context.registerService(
				ServiceDiscoveryListener.class.getName(), new Listener(), null);

		// register the listener
		final ServiceRegistration listenerReg = context.registerService(
				Service.class.getName(), new ServiceImpl(), props);

		final Set discovered = new HashSet();
		// expecting two services, one registered by the activator, one from
		// this class
		for (int i = 0; i < 2; i++) {
			synchronized (queue) {
				while (queue.isEmpty()) {
					// TODO: set timeout
					queue.wait(10000);
				}
				final String iface = (String) queue.remove(0);
				if (iface != null) {
					discovered.add(iface);
				}
			}
		}

		// both should have been discovered
		assertTrue(discovered.contains(Service.class.getName()));
		assertTrue(discovered.contains(ServiceInterface.class.getName()));

		serviceReg.unregister();
		listenerReg.unregister();
	}

	public void testServiceDiscoveryWithFilter() {
		try {
			// register the listener
			final Dictionary props2 = new Hashtable();
			props2.put(ServiceDiscoveryListener.FILTER_PROPERTY, "(foo=bar)");
			final ServiceRegistration serviceReg = context.registerService(
					ServiceDiscoveryListener.class.getName(), new Listener(),
					props2);

			// register the service
			final Dictionary props = new Hashtable();
			props.put(RemoteOSGiService.R_OSGi_REGISTRATION, Boolean.TRUE);
			props.put("foo", "bar");

			final ServiceRegistration listenerReg = context.registerService(
					Service.class.getName(), new ServiceImpl(), props);

			final Set discovered = new HashSet();
			synchronized (queue) {
				while (queue.isEmpty()) {
					// TODO: set timeout
					queue.wait(10000);
				}
				String iface = (String) queue.remove(0);
				if (iface != null) {
					discovered.add(iface);
				}
			}

			// filter should match only the first service
			assertTrue(discovered.contains(Service.class.getName()));
			assertFalse(discovered.contains(ServiceInterface.class.getName()));

			serviceReg.unregister();
			listenerReg.unregister();
		} catch (InterruptedException ie) {
			fail(ie.getMessage());
			ie.printStackTrace();
		}
	}

	public void testServiceDiscoveryForInterface() {
		try {
			// register the listener
			final Dictionary props2 = new Hashtable();
			props2.put(ServiceDiscoveryListener.SERVICE_INTERFACES_PROPERTY,
					new String[] { ServiceInterface.class.getName() });
			final ServiceRegistration serviceReg = context.registerService(
					ServiceDiscoveryListener.class.getName(), new Listener(),
					props2);

			// register the service
			final Dictionary props = new Hashtable();
			props.put(RemoteOSGiService.R_OSGi_REGISTRATION, Boolean.TRUE);
			props.put("foo", "bar");

			final ServiceRegistration listenerReg = context.registerService(
					Service.class.getName(), new ServiceImpl(), props);

			final Set discovered = new HashSet();
			synchronized (queue) {
				while (queue.isEmpty()) {
					// TODO: set timeout
					queue.wait(10000);
				}
				String iface = (String) queue.remove(0);
				if (iface == null) {
					fail();
				}
				discovered.add(iface);
				System.out.println("YES, " + iface);
			}

			// interface constraint should only match the second service
			assertFalse(discovered.contains(Service.class.getName()));
			assertTrue(discovered.contains(ServiceInterface.class.getName()));

			serviceReg.unregister();
			listenerReg.unregister();
		} catch (InterruptedException ie) {
			fail(ie.getMessage());
			ie.printStackTrace();
		}
	}

	private class Listener implements ServiceDiscoveryListener {

		public void announceService(String serviceInterface, URI uri) {
			synchronized (queue) {
				queue.add(serviceInterface);
				queue.notifyAll();
				System.out.println("GOT " + serviceInterface);
			}
		}

		public void discardService(String serviceInterface, URI uri) {

		}

	}

	private class ServiceImpl implements Service {

		public void call() {

		}

	}
}
