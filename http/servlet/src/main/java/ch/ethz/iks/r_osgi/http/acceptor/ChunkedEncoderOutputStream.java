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
package ch.ethz.iks.r_osgi.http.acceptor;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Vector;

public class ChunkedEncoderOutputStream extends BufferedOutputStream {

	// / Make a ChunkedOutputStream with a default buffer size.
	// @param out the underlying output stream
	public ChunkedEncoderOutputStream(OutputStream out) {
		super(out);
	}

	// / Make a ChunkedOutputStream with a specified buffer size.
	// @param out the underlying output stream
	// @param size the buffer size
	public ChunkedEncoderOutputStream(OutputStream out, int size) {
		super(out, size);
	}

	// / Flush the stream. This will write any buffered output
	// bytes as a chunk.
	// @exception IOException if an I/O error occurred
	public synchronized void flush() throws IOException {
		if (count != 0) {
			writeBuf(buf, 0, count);
			count = 0;
		}
	}

	private Vector footerNames = new Vector();

	private Vector footerValues = new Vector();

	// / Set a footer. Footers are much like HTTP headers, except that
	// they come at the end of the data instead of at the beginning.
	public void setFooter(String name, String value) {
		footerNames.addElement(name);
		footerValues.addElement(value);
	}

	// / Indicate the end of the chunked data by sending a zero-length chunk,
	// possible including footers.
	// @exception IOException if an I/O error occurred
	public void done() throws IOException {
		flush();
		PrintStream pout = new PrintStream(out);
		pout.println("0");
		if (footerNames.size() > 0) {
			// Send footers.
			for (int i = 0; i < footerNames.size(); ++i) {
				String name = (String) footerNames.elementAt(i);
				String value = (String) footerValues.elementAt(i);
				pout.println(name + ": " + value);
			}
		}
		footerNames = null;
		footerValues = null;
		pout.println("");
		pout.flush();
	}

	// / Make sure that calling close() terminates the chunked stream.
	public void close() throws IOException {
		if (footerNames != null)
			done();
		super.close();
	}

	// / Write a sub-array of bytes.
	// <P>
	// The only reason we have to override the BufferedOutputStream version
	// of this is that it writes the array directly to the output stream
	// if doesn't fit in the buffer. So we make it use our own chunk-write
	// routine instead. Otherwise this is identical to the parent-class
	// version.
	// @param b the data to be written
	// @param off the start offset in the data
	// @param len the number of bytes that are written
	// @exception IOException if an I/O error occurred
	public synchronized void write(byte b[], int off, int len)
			throws IOException {
		int avail = buf.length - count;

		if (len <= avail) {
			System.arraycopy(b, off, buf, count, len);
			count += len;
			return;
		}
		flush();
		writeBuf(b, off, len);
	}

	private static final byte[] crlf = { 13, 10 };

	// / The only routine that actually writes to the output stream.
	// This is where chunking semantics are implemented.
	// @exception IOException if an I/O error occurred
	private void writeBuf(byte b[], int off, int len) throws IOException {
		// Write the chunk length as a hex number.
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