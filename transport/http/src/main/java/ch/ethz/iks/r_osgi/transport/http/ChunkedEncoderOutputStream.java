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
package ch.ethz.iks.r_osgi.transport.http;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;

public class ChunkedEncoderOutputStream extends BufferedOutputStream {

	private Map footers = new TreeMap();

	public ChunkedEncoderOutputStream(final OutputStream out) {
		super(out);
	}

	public ChunkedEncoderOutputStream(final OutputStream out, final int size) {
		super(out, size);
	}

	public synchronized void flush() throws IOException {
		if (count != 0) {
			writeBuf(buf, 0, count);
			count = 0;
		}
	}

	public void setFooter(final String name, final String value) {
		footers.put(name, value);
	}

	public void done() throws IOException {
		flush();
		PrintStream pout = new PrintStream(out);
		pout.println("0");
		if (footers.size() > 0) {
			// Send footers.
			final String[] footerNames = (String[]) footers.keySet().toArray(
					new String[footers.size()]);
			for (int i = 0; i < footerNames.length; ++i) {
				String name = footerNames[i];
				String value = (String) footers.get(footerNames[i]);
				pout.println(name + ": " + value);
			}
		}
		footers.clear();
		pout.println("");
		pout.flush();
	}

	public void close() throws IOException {
		if (footers.size() > 0)
			done();
		super.close();
	}

	public synchronized void write(final byte b[], final int off, final int len)
			throws IOException {
		final int avail = buf.length - count;

		if (len <= avail) {
			System.arraycopy(b, off, buf, count, len);
			count += len;
			return;
		}
		flush();
		writeBuf(b, off, len);
	}

	private static final byte[] crlf = { 13, 10 };

	private void writeBuf(final byte b[], final int off, final int len)
			throws IOException {
		String lenStr = Integer.toString(len, 16);
		byte[] lenBytes = lenStr.getBytes();
		out.write(lenBytes);
		// Write a CRLF.
		out.write(crlf);
		// Write the data.
		if (len != 0)
			out.write(b, off, len);
		// Write a CRLF.
		out.write(crlf);
		// And flush the real stream.
		out.flush();
	}

}