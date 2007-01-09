package ch.ethz.iks.r_osgi.http.acceptor;

import javax.servlet.ServletException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

public class Activator implements BundleActivator, ServiceListener {

	private BundleContext context;

	/**
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		this.context = context;
		ServiceReference httpRef = context
				.getServiceReference(HttpService.class.getName());
		if (httpRef != null) {
			registerServlet(httpRef);
		}
		context.addServiceListener(this, "(" + Constants.OBJECTCLASS + "="
				+ HttpService.class.getName() + ")");
	}

	/**
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		context.removeServiceListener(this);
		this.context = null;
	}

	/**
	 * 
	 * @param httpRef
	 * @throws ServletException
	 * @throws NamespaceException
	 */
	private void registerServlet(ServiceReference httpRef)
			throws ServletException, NamespaceException {
		HttpService service = (HttpService) context.getService(httpRef);
		try {
			HttpAcceptorServlet servlet = new HttpAcceptorServlet();
			service.registerServlet("/r-osgi", servlet, null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
	 */
	public void serviceChanged(ServiceEvent event) {
		if (event.getType() == ServiceEvent.REGISTERED) {
			try {
				registerServlet(event.getServiceReference());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
