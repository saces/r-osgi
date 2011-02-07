package ch.ethz.iks.r_osgi.test.service.impl;

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

public abstract class TestCaseWithNestedFramework extends TestCase {

	private final FrameworkFactory fwFactory;
	protected BundleContext context;

	protected TestCaseWithNestedFramework() {
		this.fwFactory = new EquinoxFactory();
	}

	protected EmbeddedROSGiFramework newROSGiFramework(
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

	protected class EmbeddedROSGiFramework implements FrameworkListener {

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

		public RemoteOSGiService getRemoteOSGiService() {
			return remote;
		}

		public BundleContext getBundleContext() {
			return context;
		}

		public int getListeningPort() {
			return remote.getListeningPort("r-osgi");
		}

		protected void shutdown() throws BundleException, InterruptedException {
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

	protected static boolean bundleExists(final BundleContext context,
			final String b) {
		final Bundle[] bundles = context.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			if (b.equals(bundles[i].getSymbolicName())) {
				return true;
			}
		}
		return false;
	}

}
