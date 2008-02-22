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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

import ch.ethz.iks.r_osgi.RemoteOSGiService;
import ch.ethz.iks.r_osgi.RemoteServiceReference;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.sample.api.ServiceInterface;
import ch.ethz.iks.r_osgi.service_discovery.ServiceDiscoveryListener;

public class Activator implements BundleActivator {

	private ServiceReference sref;

	private RemoteOSGiService remote;

	private ServiceInterface service;

	private Thread clientThread;

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

			if (Boolean.getBoolean("ch.ethz.iks.r_osgi.service.discovery")) {
				context.registerService(ServiceDiscoveryListener.class
						.getName(), new ServiceDiscoveryListener() {

					public void announceService(String serviceInterface, URI uri) {
						remote.connect(uri);
						final RemoteServiceReference ref = remote
								.getRemoteServiceReference(uri);
						service = (ServiceInterface) remote
								.getRemoteService(ref);
						clientThread = new ClientThread();
						clientThread.start();
					}

					public void discardService(String serviceInterface, URI uri) {
						System.out.println("LOST SERVICE " + uri);

					}

				}, null);

			} else {
				final URI uri = new URI(System.getProperty(
						"ch.ethz.iks.r_osgi.service.uri",
						"r-osgi://localhost:9278"));
				// final URI uri = new URI("r-osgi://84.73.219.12:9278");
				// final URI uri = new URI("btspp://0010DCE96CB8:1");
				// final URI uri = new URI("btspp://0014A4D46D9A:1");
				// final URI uri = new URI("r-osgi://localhost:9270");
				remote.connect(uri);
				final RemoteServiceReference ref = remote
						.getRemoteServiceReference(uri);
				System.out.println("REFERENCE " + ref);
				service = (ServiceInterface) remote.getRemoteService(ref);
				clientThread = new ClientThread();
				clientThread.start();
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
						wait(5000);
					}
				}
			} catch (InterruptedException ie) {
				// let the thread terminate
			}
		}
	};

}
