/* Copyright (c) 2006-2011 Jan S. Rellermeyer
 * Systems Group, ETH Zurich.
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

package ch.ethz.iks.util;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import ch.ethz.iks.r_osgi.types.BoxedPrimitive;

/**
 * Smart object output stream that is able to deserialize classes which do not
 * implement Serializable. It only rejects classes which have native code parts
 * and the OSGi ServiceReference and ServiceRegistration classes.
 * 
 * @author Jan S. Rellermeyer
 */
public final class SmartObjectOutputStream extends ObjectOutputStream {

	private EnhancedObjectOutputStream out;

	public SmartObjectOutputStream(final OutputStream out)
			throws SecurityException, IOException {
		super();
		this.out = new EnhancedObjectOutputStream(out);
		this.flush();
	}

	protected final void writeObjectOverride(final Object o) throws IOException {
		if (o == null) {
			out.writeByte(0);
			return;
		}

		final Object obj = o instanceof BoxedPrimitive ? ((BoxedPrimitive) o)
				.getBoxed() : o;

		final String clazzName = obj.getClass().getName();
		// if (obj instanceof String) {
		// TODO:
		// out.writeUTF((String) obj);
		// return;
		// } else
		if (SmartConstants.positiveList.contains(clazzName)) {
			// string serializable classes
			out.writeByte(1);
			final String id = (String) SmartConstants.classToId.get(clazzName);

			out.write(id == null ? 0 : id.getBytes()[0]);
			if (id == null) {
				out.writeUTF(id != null ? id : clazzName);
			}
			out.writeUTF(obj.toString());
			return;
		} else if (obj.getClass().isArray()) {
			// arrays
			out.writeByte(2);
			out.writeInt(Array.getLength(obj));
			out.writeUTF(obj.getClass().getName());

			for (int i = 0; i < Array.getLength(obj); i++) {
				writeObjectOverride(Array.get(obj, i));
			}
		} else {
			// java serializable classes
			out.writeByte(3);
			out.writeObject(obj);
			return;
		}
	}

	/**
	 * 
	 * @see java.io.ObjectOutputStream#write(int)
	 */
	public final void write(final int val) throws IOException {
		out.write(val);
	}

	/**
	 * 
	 * @see java.io.ObjectOutputStream#write(byte[])
	 */
	public final void write(final byte[] buf) throws IOException {
		out.write(buf);
	}

	/**
	 * 
	 * @see java.io.ObjectOutputStream#write(byte[], int, int)
	 */
	public final void write(final byte[] buf, final int off, final int len)
			throws IOException {
		out.write(buf, off, len);
	}

	/**
	 * 
	 * @see java.io.ObjectOutputStream#flush()
	 */
	public final void flush() throws IOException {
		out.flush();
	}

	/**
	 * 
	 * @see java.io.ObjectOutputStream#reset()
	 */
	public final void reset() throws IOException {
		out.reset();
	}

	/**
	 * 
	 * @see java.io.ObjectOutputStream#close()
	 */
	public final void close() throws IOException {
		out.close();
	}

	/**
	 * 
	 * @see java.io.ObjectOutputStream#writeBoolean(boolean)
	 */
	public final void writeBoolean(final boolean val) throws IOException {
		out.writeBoolean(val);
	}

	/**
	 * 
	 * @see java.io.ObjectOutputStream#writeByte(int)
	 */
	public final void writeByte(final int val) throws IOException {
		out.writeByte(val);
	}

	/**
	 * 
	 * @see java.io.ObjectOutputStream#writeShort(int)
	 */
	public final void writeShort(final int val) throws IOException {
		out.writeShort(val);
	}

	/**
	 * 
	 * @see java.io.ObjectOutputStream#writeChar(int)
	 */
	public final void writeChar(final int val) throws IOException {
		out.writeChar(val);
	}

	/**
	 * 
	 * @see java.io.ObjectOutputStream#writeInt(int)
	 */
	public final void writeInt(final int val) throws IOException {
		out.writeInt(val);
	}

	/**
	 * 
	 * @see java.io.ObjectOutputStream#writeLong(long)
	 */
	public final void writeLong(final long val) throws IOException {
		out.writeLong(val);
	}

	/**
	 * 
	 * @see java.io.ObjectOutputStream#writeFloat(float)
	 */
	public final void writeFloat(final float val) throws IOException {
		out.writeFloat(val);
	}

	/**
	 * 
	 * @see java.io.ObjectOutputStream#writeDouble(double)
	 */
	public final void writeDouble(final double val) throws IOException {
		out.writeDouble(val);
	}

	/**
	 * 
	 * @see java.io.ObjectOutputStream#writeBytes(java.lang.String)
	 */
	public final void writeBytes(final String str) throws IOException {
		out.writeBytes(str);
	}

	/**
	 * 
	 * @see java.io.ObjectOutputStream#writeChars(java.lang.String)
	 */
	public final void writeChars(final String str) throws IOException {
		out.writeChars(str);
	}

	/**
	 * 
	 * @see java.io.ObjectOutputStream#writeUTF(java.lang.String)
	 */
	public final void writeUTF(final String str) throws IOException {
		out.writeUTF(str);
	}

	static class EnhancedObjectOutputStream extends ObjectOutputStream {

		public EnhancedObjectOutputStream(final OutputStream out)
				throws IOException {
			super(new EnhancedGZIPOutputStream(out));
			this.enableReplaceObject(true);
		}

		protected Object replaceObject(final Object obj) throws IOException {
			if (obj instanceof Serializable) {
				return obj;
			}

			final Class clazz = obj.getClass();
			if (SmartConstants.blackList.contains(clazz.getName())) {
				throw new NotSerializableException(clazz.getName());
			}

			return new SmartObjectStreamClass(obj, clazz);
		}

		static class EnhancedGZIPOutputStream extends GZIPOutputStream {

			private static final byte[] NOTHING = new byte[0];
			private boolean hasPendingBytes = false;

			public EnhancedGZIPOutputStream(final OutputStream out)
					throws IOException {
				super(out);
				def.setLevel(Deflater.BEST_SPEED);
			}

			public void write(final byte[] bytes, final int i, final int i1)
					throws IOException {
				super.write(bytes, i, i1);
				hasPendingBytes = true;
			}

			public synchronized void write(final int i) throws IOException {
				super.write(i);
				hasPendingBytes = true;
			}

			public synchronized void write(final byte[] bytes)
					throws IOException {
				super.write(bytes);
				hasPendingBytes = true;
			}

			protected void deflate() throws IOException {
				int len;
				do {
					len = def.deflate(buf, 0, buf.length);
					if (len == 0) {
						break;
					}
					this.out.write(buf, 0, len);
				} while (true);
			}

			public void flush() throws IOException {
				if (!hasPendingBytes) {
					return;
				}

				if (!def.finished()) {
					def.setInput(NOTHING, 0, 0);
					def.setLevel(Deflater.NO_COMPRESSION);
					deflate();
					def.setLevel(Deflater.BEST_SPEED);
					deflate();
					super.flush();
				}

				hasPendingBytes = false;
			}
		}
	}
}
