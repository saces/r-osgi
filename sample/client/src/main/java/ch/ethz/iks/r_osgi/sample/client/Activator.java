/* Copyright (c) 2006-2008 Jan S. Rellermeyer
 * Information and Communication Systems Research Group (IKS),
 * Department of Computer Science, ETH Zurich.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *    - Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    - Neither the name of ETH Zurich nor the names of its contributors may be
 *      used to endorse or promote products derived from this software without
 *      specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package ch.ethz.iks.r_osgi.sample.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Filter;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import ch.ethz.iks.r_osgi.RemoteOSGiException;
import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.RemoteServiceReference;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.sample.api.ServiceInterface;
import ch.ethz.iks.r_osgi.service_discovery.ServiceDiscoveryListener;

public class Activator implements BundleActivator, EventHandler {

	private ServiceReference sref;

	private static RemoteOSGiService remote;

	private static ServiceInterface service;

	private Thread clientThread;

	private static final int GET_PROXY = 0;

	private static final int GET_BUNDLE_CLONE = 1;

	private static final int CLIENT = GET_PROXY;

	private static final URI uri = new URI(System.getProperty(
			"ch.ethz.iks.r_osgi.service.uri", "r-osgi://localhost:9278"));

	// private static final URI uri = new URI("btspp://0010DCE96CB8:1");
	// private static final URI uri = new URI("btspp://0014A4D46D9A:1");
	// private static final URI uri = new URI("r-osgi://localhost:9270");

	public void start(final BundleContext context) {
		try {
			System.out.println("starting sample client");

			sref = context.getServiceReference(RemoteOSGiService.class
					.getName());
			if (sref != null) {
				remote = (RemoteOSGiService) context.getService(sref);
			} else {
				throw new BundleException("OSGi remote service is not present.");
			}

			final Hashtable props = new Hashtable();
			props.put(EventConstants.EVENT_TOPIC, new String[] { "test/*" });
			context.registerService(EventHandler.class.getName(), this, props);

			if (Boolean.getBoolean("ch.ethz.iks.r_osgi.service.discovery")) {
				context.registerService(
						ServiceDiscoveryListener.class.getName(),
						new ServiceDiscoveryListener() {

							public void announceService(
									String serviceInterface, URI uri) {
								try {
									remote.connect(uri);
									final RemoteServiceReference ref = remote
											.getRemoteServiceReference(uri);
									service = (ServiceInterface) remote
											.getRemoteService(ref);
									clientThread = new ClientThread();
									clientThread.start();
								} catch (IOException ioe) {
									ioe.printStackTrace();
								}
							}

							public void discardService(String serviceInterface,
									URI uri) {
								System.out.println("LOST SERVICE " + uri);

							}

						}, null);

			} else {

				// wait for the proxy to disappear in order to do a reconnect
				final ServiceTracker tracker = new ServiceTracker(context,
						ServiceInterface.class.getName(),
						new ServiceTrackerCustomizer() {

							public Object addingService(
									final ServiceReference reference) {
								clientThread = new ClientThread();
								clientThread.start();
								return context.getService(reference);
							}

							public void modifiedService(
									ServiceReference reference, Object service) {

							}

							public void removedService(
									ServiceReference reference, Object service) {
								clientThread.interrupt();
								// service disappeared, try a reconnect
								new ReconnectThread().start();
							}

						});
				tracker.open();

				getService();

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stop(final BundleContext context) throws Exception {
		// if the thread exists, interrupt it
		if (clientThread != null) {
			clientThread.interrupt();
			clientThread = null;
		}
		remote = null;
	}

	private static final void getService() throws Exception {
		remote.connect(uri);
		final RemoteServiceReference[] refs = remote
				.getRemoteServiceReferences(uri,
						ServiceInterface.class.getName(), null);
		System.out.println("REFERENCES " + Arrays.asList(refs));

		if (CLIENT == GET_PROXY) {
			service = (ServiceInterface) remote.getRemoteService(refs[0]);
		} else if (CLIENT == GET_BUNDLE_CLONE) {
			service = (ServiceInterface) remote.getRemoteServiceBundle(refs[0],
					0);
			System.out.println("SERVICE IS " + service);
		}
	}

	private class ClientThread extends Thread {
		public void run() {
			setName("SampleClientThread");
			try {
				int i = 1;
				while (!isInterrupted()) {
					synchronized (this) {
						System.out.println("Invoking remote service:");
						System.out.println(service.echoService("my message",
								new Integer(i)));
						System.out
								.println(service.reverseService("my message"));
						System.out.println("calling local");
						try {
							service.local();
						} catch (RuntimeException r) {
							r.printStackTrace();
						}
						service.printRemote(i, 0.987654321F);
						System.out.println(service.equals(new Integer(10)));
						if (i <= 10) {
							i++;
						}
						service.verifyBlock("This is a test".getBytes(), 0, 1,
								2);

						service.echoByteArray1("great test".getBytes());

						service.echoByteArray2(new byte[][] { "one".getBytes(),
								"two".getBytes(), "three".getBytes() });

						System.out
								.println(service.checkArray("ABCDEF", 1000)[0]);

						System.out.println(service.checkDoubleArray("FEDCBA",
								100, 100)[1][1]);
						wait(5000);
					}
				}
			} catch (InterruptedException ie) {
				// let the thread terminate
			}
		}
	}

	public class ReconnectThread extends Thread {
		public void run() {
			while (true) {
				try {
					remote.connect(uri);
					System.err.println("reconnect successful");
					break;
				} catch (RemoteOSGiException e) {
					System.err.println("reconnect not successful");
				} catch (IOException e) {
					System.err.println("reconnect not successful");
				}
			}

			try {
				getService();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void handleEvent(Event event) {
		System.out.println("---> Received event " + event.getTopic());
	};

}
