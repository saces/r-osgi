package ch.ethz.iks.r_osgi.sample.http.service;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.sample.api.ServiceInterface;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		Hashtable properties = new Hashtable();
		properties.put(RemoteOSGiService.R_OSGi_REGISTRATION,
				RemoteOSGiService.USE_PROXY_POLICY);
		context.registerService(ServiceInterface.class.getName(),
				new TestServiceImpl(), properties);
	}

	public void stop(BundleContext context) throws Exception {

	}

}
