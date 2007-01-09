/* Copyright (c) 2006-2007 Jan S. Rellermeyer
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
package ch.ethz.iks.r_osgi.impl;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

/**
 * <p>
 * DeliverBundleMessage is used to bring an OSGi service to a remote machine.
 * </p>
 * 
 * @author Jan S. Rellermeyer, ETH Zurich.
 * @since 0.5
 */
final class DeliverBundleMessage extends RemoteOSGiMessageImpl {

	/**
	 * The raw bytes of the bundle.
	 */
	private byte[] bytes;

	/**
	 * Hidden default constructor.
	 */
	private DeliverBundleMessage() {

	}

	/**
	 * Create a new DeliverBundleMessage for a service registered with
	 * TRANSFER_BUNDLE_POLICY.
	 * 
	 * @param fetchReq
	 *            the <code>FetchServiceMessage</code>.
	 * @param reg
	 *            the <code>BundledServiceRegistration</code>.
	 * @throws IOException
	 *             in case of IO errors.
	 */
	DeliverBundleMessage(final FetchServiceMessage fetchReq,
			final BundledServiceRegistration reg) throws IOException {
		xid = fetchReq.getXID();
		funcID = DELIVER_BUNDLE;
		this.bytes = reg.getBundle();
	}

	/**
	 * Create a new DeliverBundleMessage from network packet.
	 * 
	 * <pre>
	 *        0                   1                   2                   3
	 *        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *       |       R-OSGi header (function = DeliverBundle = 8)            |
	 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 *       |   length of &lt;Bundle&gt;      |     &lt;Bundle&gt; Bytes        \
	 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 * </pre>
	 * 
	 * @param input
	 *            a <code>DataInput</code> that provides the body of a R-OSGi
	 *            network packet.
	 * @throws IOException
	 *             in case of parse errors.
	 */
	DeliverBundleMessage(final ObjectInputStream input) throws IOException {
		// read the bytes
		bytes = readBytes(input);
	}

	/**
	 * Write the body of the DeliverServiceMessage as raw bytes to an output
	 * stream.
	 * 
	 * @param out the ObjectOutputStream.
	 * @throws IOException
	 *             in case of parse errors.
	 */
	public void writeBody(final ObjectOutputStream out) throws IOException {
		writeBytes(out, bytes);
	}

	/**
	 * get the bytes.
	 * 
	 * @return the raw bytes of the bundle.
	 */
	byte[] getBundle() {
		return bytes;
	}

	/**
	 * String representation for debug outputs.
	 * 
	 * @return a string representation.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[DELIVER_BUNDLE] - XID: ");
		buffer.append(xid);
		buffer.append(", ");
		buffer.append(bytes.length);
		buffer.append(" bytes of bundle data");
		return buffer.toString();
	}
}
