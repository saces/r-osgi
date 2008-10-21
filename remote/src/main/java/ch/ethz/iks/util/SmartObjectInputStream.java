package ch.ethz.iks.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class SmartObjectInputStream extends ObjectInputStream {

	private final ObjectInputStream in;

	public SmartObjectInputStream(final InputStream in) throws IOException {
		this.in = new ObjectInputStream(in);
	}

	protected Object readObjectOverride() throws IOException,
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
						System.out.println("read " + fieldName);
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
				throw new IOException("Error while deserializing " + clazzName
						+ ": " + e.getMessage());
			}
		default:
			System.err.println("Unhandled case " + cat);
			throw new IllegalStateException();
		}
	}

	public int read() throws IOException {
		return in.read();
	}

	public int read(final byte[] buf, final int off, final int len)
			throws IOException {
		return in.read(buf, off, len);
	}

	public int available() throws IOException {
		return in.available();
	}

	public void close() throws IOException {
		in.close();
	}

	public boolean readBoolean() throws IOException {
		return in.readBoolean();
	}

	public byte readByte() throws IOException {
		return in.readByte();
	}

	public int readUnsignedByte() throws IOException {
		return in.readUnsignedByte();
	}

	public char readChar() throws IOException {
		return in.readChar();
	}

	public short readShort() throws IOException {
		return in.readShort();
	}

	public int readUnsignedShort() throws IOException {
		return in.readUnsignedShort();
	}

	public int readInt() throws IOException {
		return in.readInt();
	}

	public long readLong() throws IOException {
		return in.readLong();
	}

	public float readFloat() throws IOException {
		return in.readFloat();
	}

	public double readDouble() throws IOException {
		return in.readDouble();
	}

	public void readFully(final byte[] buf) throws IOException {
		in.readFully(buf, 0, buf.length);
	}

	public void readFully(final byte[] buf, final int off, final int len)
			throws IOException {
		in.readFully(buf, off, len);
	}

	public int skipBytes(final int len) throws IOException {
		return in.skipBytes(len);
	}

	/**
	 * @deprecated
	 */
	public String readLine() throws IOException {
		return in.readLine();
	}

	public String readUTF() throws IOException {
		return in.readUTF();
	}

}
