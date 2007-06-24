/* Copyright (c) 2006 Jan S. Rellermeyer
 * Information and Communication Systems Research Group (IKS),
 * Institute for Pervasive Computing, ETH Zurich.
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
 * 
 */
package ch.ethz.iks.r_osgi.serviceUI;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import ch.ethz.iks.r_osgi.RemoteServiceListener;
import ch.ethz.iks.r_osgi.RemoteOSGiService;

public class ServiceUIActivator implements BundleActivator {
	static RemoteOSGiService remote;

	private ServiceUI serviceUI;

	static BundleContext context;

	public void start(BundleContext context) throws Exception {
		ServiceUIActivator.context = context;
		ServiceReference ref = context
				.getServiceReference(RemoteOSGiService.class.getName());
		if (ref != null) {
			remote = (RemoteOSGiService) context.getService(ref);
			serviceUI = new ServiceUI();
			context.registerService(RemoteServiceListener.class.getName(),
					serviceUI, null);
		} else {
			throw new RuntimeException("Could not retrieve RemoteOSGiService");
		}
	}

	public void stop(BundleContext context) throws Exception {
		serviceUI.dispose();
		context = null;
	}

}
