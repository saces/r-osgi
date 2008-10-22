/* Copyright (c) 2006-2008 Jan S. Rellermeyer
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class SmartObjectInputStream extends ObjectInputStream {

	private final ObjectInputStream in;

	public SmartObjectInputStream(final InputStream in) throws IOException {
		this.in = new ObjectInputStream(in);
	}

	protected final Object readObjectOverride() throws IOException,
			ClassNotFoundException {
		final int cat = in.read();
		switch (cat) {
		case 0:
			return null;
		case 1:
			// string serialized object
			try {
				final String type = in.readUTF();
				final Class test = (Class) SmartConstants.idToClass.get(type);
				final Class clazz = test != null ? test : Class.forName(type);
				final Constructor constr = clazz
						.getConstructor(new Class[] { String.class });
				return constr.newInstance(new Object[] { in.readUTF() });
			} catch (final Exception e) {
				e.printStackTrace();
				throw new IOException(e.getMessage());
			}
		case 2:
			// java serialized object
			return in.readObject();
		case 3:
			// smart serialized object
			final String clazzName = in.readUTF();

			// TODO: cache this information...
			Class clazz = Class.forName(clazzName);

			try {
				final Constructor constr = clazz.getDeclaredConstructor(null);
				constr.setAccessible(true);
				final Object newInstance = constr.newInstance(null);

				int fieldCount = in.readInt();
				while (fieldCount > -1) {
					for (int i = 0; i < fieldCount; i++) {
						final String fieldName = in.readUTF();
						System.out.println("read " + fieldName); //$NON-NLS-1$
						final Object value = readObjectOverride();
						final Field field = clazz.getDeclaredField(fieldName);

						final int mod = field.getModifiers();
						if (!Modifier.isPublic(mod)) {
							field.setAccessible(true);
						}

						field.set(newInstance, value);
					}
					clazz = clazz.getSuperclass();
					fieldCount = in.readInt();
				}
				return newInstance;
			} catch (final Exception e) {
				e.printStackTrace();
				throw new IOException("Error while deserializing " + clazzName //$NON-NLS-1$
						+ ": " + e.getMessage()); //$NON-NLS-1$
			}
		default:
			System.err.println("Unhandled case " + cat); //$NON-NLS-1$
			throw new IllegalStateException();
		}
	}

	public final int read() throws IOException {
		return in.read();
	}

	public final int read(final byte[] buf, final int off, final int len)
			throws IOException {
		return in.read(buf, off, len);
	}

	public final int available() throws IOException {
		return in.available();
	}

	public final void close() throws IOException {
		in.close();
	}

	public final boolean readBoolean() throws IOException {
		return in.readBoolean();
	}

	public final byte readByte() throws IOException {
		return in.readByte();
	}

	public final int readUnsignedByte() throws IOException {
		return in.readUnsignedByte();
	}

	public final char readChar() throws IOException {
		return in.readChar();
	}

	public final short readShort() throws IOException {
		return in.readShort();
	}

	public final int readUnsignedShort() throws IOException {
		return in.readUnsignedShort();
	}

	public final int readInt() throws IOException {
		return in.readInt();
	}

	public final long readLong() throws IOException {
		return in.readLong();
	}

	public final float readFloat() throws IOException {
		return in.readFloat();
	}

	public final double readDouble() throws IOException {
		return in.readDouble();
	}

	public final void readFully(final byte[] buf) throws IOException {
		in.readFully(buf, 0, buf.length);
	}

	public final void readFully(final byte[] buf, final int off, final int len)
			throws IOException {
		in.readFully(buf, off, len);
	}

	public final int skipBytes(final int len) throws IOException {
		return in.skipBytes(len);
	}

	/**
	 * @deprecated
	 */
	public final String readLine() throws IOException {
		return in.readLine();
	}

	public final String readUTF() throws IOException {
		return in.readUTF();
	}

}
