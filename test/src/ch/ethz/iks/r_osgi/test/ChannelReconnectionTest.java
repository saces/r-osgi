package ch.ethz.iks.r_osgi.test;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

import junit.framework.TestCase;

import org.eclipse.osgi.launch.EquinoxFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.RemoteServiceReference;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.sample.api.ServiceInterface;

public class ChannelReconnectionTest extends TestCase {

	private BundleContext context;
	private RemoteOSGiService remote;
	private Bundle rosgiBundle;

	private FrameworkFactory fwFactory;

	protected void setUp() throws Exception {
		super.setUp();

		context = Activator.getActivator().getContext();
		remote = Activator.getActivator().getR_OSGi();
		rosgiBundle = Activator.getActivator().getR_OSGiBundle();

		fwFactory = new EquinoxFactory();
	}

	protected void tearDown() throws Exception {
		super.tearDown();

		context = null;
		remote = null;

		fwFactory = null;
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
		assertEquals(1, rrefs.length);

		final ServiceInterface service = (ServiceInterface) r1
				.getRemoteService(rrefs[0]);

		assertTrue(bundleExists(fw1.context,
				"proxy for r-osgi://localhost:9278#" + serviceID));

		rosgiBundle.stop();

		assertFalse(bundleExists(fw1.context,
				"proxy for r-osgi://localhost:9278#" + serviceID));

		rosgiBundle.start();

		final RemoteServiceReference[] rrefs2 = r1.connect(new URI(
				"r-osgi://localhost:9278"));
	}

	private EmbeddedROSGiFramework newROSGiFramework(
			final String[] targetBundles) throws BundleException, Exception {
		final HashSet set = new HashSet(Arrays.asList(targetBundles));
		set.add("ch.ethz.iks.r_osgi.remote");
		set.add("org.eclipse.osgi.services");
		final Framework fw = newFrameworkWithBundles(set);
		return new EmbeddedROSGiFramework(fw);
	}

	private Framework newFrameworkWithBundles(final Set targetBundles)
			throws BundleException {
		final HashMap props = new HashMap();
		props.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "2");

		final Framework fw = fwFactory.newFramework(props);
		fw.init();

		final BundleContext newContext = fw.getBundleContext();

		final Bundle[] bundles = context.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			final String symbolicName = bundles[i].getSymbolicName();
			if (targetBundles.contains(symbolicName)) {
				final Bundle b = newContext.installBundle(bundles[i]
						.getLocation().replace("initial@", ""));
				b.start();
			}
		}
		return fw;
	}

	class ReflectiveDecoupling implements InvocationHandler {

		private final Object proxied;

		private final HashMap methodTable;

		private final Class uriClass;

		private final ClassLoader target;

		private WeakHashMap translationTable;

		public ReflectiveDecoupling(final Object proxied,
				final ClassLoader target) throws ClassNotFoundException {
			this.proxied = proxied;
			this.target = target;
			methodTable = new HashMap();
			final Class clazz = proxied.getClass();
			final Method[] methods = clazz.getMethods();
			for (int i = 0; i < methods.length; i++) {
				final Method m = methods[i];
				methodTable.put(getMethodString(m), m);
			}
			uriClass = proxied.getClass().getClassLoader()
					.loadClass(URI.class.getName());
			translationTable = new WeakHashMap();
		}

		public Object invoke(final Object proxy, final Method method,
				final Object[] args) throws Throwable {
			final Method m = (Method) methodTable.get(getMethodString(method));
			m.setAccessible(true);
			return translate(m.invoke(proxied, translate(args)),
					m.getReturnType());
		}

		private String getMethodString(final Method m) {
			return m.getName()
					+ Arrays.asList(m.getParameterTypes()).toString();
		}

		private Object[] translate(final Object[] args)
				throws IllegalArgumentException, SecurityException,
				InstantiationException, IllegalAccessException,
				InvocationTargetException, NoSuchMethodException {
			if (args == null) {
				return args;
			}

			final Object[] translated = new Object[args.length];
			for (int i = 0; i < args.length; i++) {
				final Object o = args[i];
				// if (o == null) {
				// translated[i] = null;
				// continue;
				// }

				final Object t = translationTable.get(o);
				if (t != null) {
					translated[i] = t;
				} else if (o.getClass().getName().equals(URI.class.getName())) {
					translated[i] = uriClass.getConstructor(
							new Class[] { String.class }).newInstance(
							new Object[] { ((URI) o).toString() });
				} else {
					translated[i] = o;
				}
			}
			return translated;
		}

		private Object translate(final Object arg, final Class type)
				throws Exception {
			if (arg.getClass().getClassLoader() == null) {
				return arg;
			}

			if (arg.getClass().isArray()) {
				final Object translated = Array.newInstance(
						target.loadClass(type.getComponentType().getName()),
						Array.getLength(arg));

				for (int i = 0; i < Array.getLength(arg); i++) {
					Array.set(translated, i, translate(Array.get(arg, i), type));
				}

				return translated;
			}

			if (arg.getClass()
					.getName()
					.equals("ch.ethz.iks.r_osgi.impl.RemoteServiceReferenceImpl")) {
				final Object proxy = Proxy.newProxyInstance(target,
						new Class[] { RemoteServiceReference.class },
						new ReflectiveDecoupling(arg, target));
				translationTable.put(proxy, arg);
				return proxy;
			}

			// System.err.println("NO SUBSTITUTION FOR "
			// + arg.getClass().getName() + " - " + type);

			return null;
		}
	}

	class EmbeddedROSGiFramework implements FrameworkListener {

		private final Framework fw;
		private final RemoteOSGiService remote;
		private final BundleContext context;

		EmbeddedROSGiFramework(final Framework fw)
				throws IllegalArgumentException, ClassNotFoundException,
				BundleException {
			this.fw = fw;
			context = fw.getBundleContext();

			context.addFrameworkListener(this);
			fw.start();

			final ServiceReference ref = context
					.getServiceReference(RemoteOSGiService.class.getName());

			if (ref == null) {
				throw new IllegalStateException("RemoteOSGiService not found");
			}

			final Object fwRemote = context.getService(ref);
			remote = (RemoteOSGiService) Proxy.newProxyInstance(
					this.getClass().getClassLoader(),
					new Class[] { RemoteOSGiService.class },
					new ReflectiveDecoupling(fwRemote, RemoteOSGiService.class
							.getClassLoader()));
		}

		RemoteOSGiService getRemoteOSGiService() {
			return remote;
		}

		BundleContext getBundleContext() {
			return context;
		}

		int getListeningPort() {
			return remote.getListeningPort("r-osgi");
		}

		void shutdown() throws BundleException, InterruptedException {
			fw.stop();
			fw.waitForStop(0);
		}

		public void frameworkEvent(final FrameworkEvent event) {
			switch (event.getType()) {
			case FrameworkEvent.STARTED:
				System.err.println("Framework started");
				break;
			case FrameworkEvent.STARTLEVEL_CHANGED:
				System.err.println("Framework startlevel changed");
				break;
			case FrameworkEvent.ERROR:
				System.err.println(event.getThrowable().getMessage());
				break;
			default:
				System.err.println(event);
			}
		}

	}

	private boolean bundleExists(final BundleContext context, final String b) {
		final Bundle[] bundles = context.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			if (b.equals(bundles[i].getSymbolicName())) {
				return true;
			}
		}
		return false;
	}

}
