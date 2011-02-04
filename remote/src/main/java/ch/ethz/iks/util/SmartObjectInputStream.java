/* Copyright (c) 2006-2009 Jan S. Rellermeyer
 * Systems Group,
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
 */

package ch.ethz.iks.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Smart object input stream that is able to deserialize classes which do not
 * implement Serializable. It only rejects classes which have native code parts
 * and the OSGi ServiceReference and ServiceRegistration classes.
 * 
 * @author Jan S. Rellermeyer
 * 
 */
public final class SmartObjectInputStream extends ObjectInputStream {

	final EnhancedObjectInputStream in;

	public SmartObjectInputStream(final InputStream in)
			throws SecurityException, IOException {
		super();
		this.in = new EnhancedObjectInputStream(in);
	}

	protected final Object readObjectOverride() throws IOException,
			ClassNotFoundException {

		final byte cat = in.readByte();
		switch (cat) {
		case 0:
			// null
			return null;
		case 1:
			// string serialized object
			// TODO: cache constructors
			try {
				final byte b = (byte) in.read();
				final Class clazz;
				if (b == 0) {
					final String type = in.readUTF();
					clazz = Class.forName(type);
				} else {
					clazz = (Class) SmartConstants.idToClass.get(String
							.valueOf(b));
				}
				final Constructor constr = clazz
						.getConstructor(new Class[] { String.class });
				return constr.newInstance(new Object[] { in.readUTF() });
			} catch (final Exception e) {
				e.printStackTrace();
				throw new IOException(e.getMessage());
			}
		case 3:
			// java serialized object
			return in.readObject();
		case 2:
			final int size = in.readInt();
			final String type = in.readUTF();

			final Object newInstance = Array.newInstance(Class.forName(type)
					.getComponentType(), size);

			for (int i = 0; i < size; i++) {
				Array.set(newInstance, i, readObjectOverride());
			}

			return newInstance;
		default:
			throw new IllegalStateException("Unhandled case " + cat); //$NON-NLS-1$
		}
	}

	/**
	 * 
	 * @see java.io.ObjectInputStream#read()
	 */
	public final int read() throws IOException {
		return in.read();
	}

	/**
	 * 
	 * @see java.io.ObjectInputStream#read(byte[], int, int)
	 */
	public final int read(final byte[] buf, final int off, final int len)
			throws IOException {
		return in.read(buf, off, len);
	}

	/**
	 * 
	 * @see java.io.ObjectInputStream#available()
	 */
	public final int available() throws IOException {
		return in.available();
	}

	/**
	 * 
	 * @see java.io.ObjectInputStream#close()
	 */
	public final void close() throws IOException {
		in.close();
	}

	/**
	 * 
	 * @see java.io.ObjectInputStream#readBoolean()
	 */
	public final boolean readBoolean() throws IOException {
		return in.readBoolean();
	}

	/**
	 * 
	 * @see java.io.ObjectInputStream#readByte()
	 */
	public final byte readByte() throws IOException {
		return in.readByte();
	}

	/**
	 * 
	 * @see java.io.ObjectInputStream#readUnsignedByte()
	 */
	public final int readUnsignedByte() throws IOException {
		return in.readUnsignedByte();
	}

	/**
	 * 
	 * @see java.io.ObjectInputStream#readChar()
	 */
	public final char readChar() throws IOException {
		return in.readChar();
	}

	/**
	 * 
	 * @see java.io.ObjectInputStream#readShort()
	 */
	public final short readShort() throws IOException {
		return in.readShort();
	}

	/**
	 * 
	 * @see java.io.ObjectInputStream#readUnsignedShort()
	 */
	public final int readUnsignedShort() throws IOException {
		return in.readUnsignedShort();
	}

	/**
	 * 
	 * @see java.io.ObjectInputStream#readInt()
	 */
	public final int readInt() throws IOException {
		return in.readInt();
	}

	/**
	 * 
	 * @see java.io.ObjectInputStream#readLong()
	 */
	public final long readLong() throws IOException {
		return in.readLong();
	}

	/**
	 * 
	 * @see java.io.ObjectInputStream#readFloat()
	 */
	public final float readFloat() throws IOException {
		return in.readFloat();
	}

	/**
	 * 
	 * @see java.io.ObjectInputStream#readDouble()
	 */
	public final double readDouble() throws IOException {
		return in.readDouble();
	}

	/**
	 * 
	 * @see java.io.ObjectInputStream#readFully(byte[])
	 */
	public final void readFully(final byte[] buf) throws IOException {
		in.readFully(buf);
	}

	/**
	 * 
	 * @see java.io.ObjectInputStream#readFully(byte[], int, int)
	 */
	public final void readFully(final byte[] buf, final int off, final int len)
			throws IOException {
		in.readFully(buf, off, len);
	}

	/**
	 * 
	 * @see java.io.ObjectInputStream#skipBytes(int)
	 */
	public final int skipBytes(final int len) throws IOException {
		return in.skipBytes(len);
	}

	/**
	 * @deprecated
	 */
	public final String readLine() throws IOException {
		return in.readLine();
	}

	/**
	 * 
	 * @see java.io.ObjectInputStream#readUTF()
	 */
	public final String readUTF() throws IOException {
		return in.readUTF();
	}

	class EnhancedObjectInputStream extends ObjectInputStream {

		private Object handles;
		private Field handle;
		private Method setHandle;

		EnhancedObjectInputStream(final InputStream in) throws IOException {
			super(in);
			enableResolveObject(true);
			try {
				final Field field = getClass().getSuperclass()
						.getDeclaredField("handles");
				field.setAccessible(true);
				handles = field.get(this);
				handle = getClass().getSuperclass().getDeclaredField(
						"passHandle");
				handle.setAccessible(true);
				setHandle = handles.getClass().getDeclaredMethod("setObject",
						new Class[] { Integer.TYPE, Object.class });
				setHandle.setAccessible(true);
			} catch (Exception e) {
				// handle replacement won't work.
			}
		}

		protected Object resolveObject(final Object obj) throws IOException {
			if (obj instanceof SmartObjectStreamClass) {
				try {
					return ((SmartObjectStreamClass) obj).restoreObject();
				} catch (Exception e) {
					final IOException f = new IOException(
							"Exception while resolving object");
					f.initCause(e);
					throw f;
				}
			}
			return obj;
		}

		void fixHandle(final Object obj) {
			if (setHandle == null) {
				return;
			}
			try {
				setHandle.invoke(handles,
						new Object[] { handle.get(this), obj });
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
