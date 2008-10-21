package ch.ethz.iks.util;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import ch.ethz.iks.r_osgi.types.BoxedPrimitive;

public class SmartObjectOutputStream extends ObjectOutputStream {

	private final ObjectOutputStream out;

	public SmartObjectOutputStream(final OutputStream out) throws IOException {
		this.out = new ObjectOutputStream(out);
	}

	protected void writeObjectOverride(Object obj) throws IOException {
		if (obj == null) {
			out.write(0);
			return;
		}
		if (obj instanceof BoxedPrimitive) {
			obj = ((BoxedPrimitive) obj).getBoxed();
		}

		final String clazzName = obj.getClass().getName();
		if (SmartConstants.positiveList.contains(clazzName)) {
			// string serializable classes
			out.write(1);
			final String id = (String) SmartConstants.classToId.get(clazzName);
			out.writeUTF(id != null ? id : clazzName);
			out.writeUTF(obj.toString());
			return;
		} else if (obj instanceof Serializable) {
			// java serializable classes
			out.write(2);
			out.writeObject(obj);
		} else {
			out.write(3);

			// all other classes: try smart serialization
			Class clazz = obj.getClass();

			if (SmartConstants.blackList.contains(clazz.getName())) {
				throw new NotSerializableException("Class " + clazz.getName()
						+ " is not serializable");
			}

			out.writeUTF(clazz.getName());

			// TODO: cache this information...

			while (clazz != Object.class) {
				// check for native methods
				final Method[] methods = clazz.getDeclaredMethods();
				for (int j = 0; j < methods.length; j++) {
					final int mod = methods[j].getModifiers();
					if (Modifier.isNative(mod)) {
						throw new NotSerializableException(
								"Class "
										+ clazz.getName()
										+ " contains native methods and is therefore not serializable."); //$NON-NLS-1$ 
					}
				}

				try {
					final Field[] fields = clazz.getDeclaredFields();
					final int fieldCount = fields.length;
					out.writeInt(fieldCount);
					for (int i = 0; i < fieldCount; i++) {
						final int mod = fields[i].getModifiers();
						if (Modifier.isStatic(mod)) {
							continue;
						} else if (!Modifier.isPublic(mod)) {
							fields[i].setAccessible(true);
						}
						out.writeUTF(fields[i].getName());
						writeObjectOverride(fields[i].get(obj));
					}
				} catch (final Exception e) {
					throw new NotSerializableException(
							"Exception while serializing " + obj.toString()
									+ ":\n" + e.getMessage()); //$NON-NLS-1$ 
				}
				clazz = clazz.getSuperclass();
			}
			out.writeInt(-1);
		}
	}

	public void write(final int val) throws IOException {
		out.write(val);
	}

	public void write(final byte[] buf) throws IOException {
		out.write(buf, 0, buf.length);
	}

	public void write(final byte[] buf, final int off, final int len)
			throws IOException {
		out.write(buf, off, len);
	}

	public void flush() throws IOException {
		out.flush();
	}

	public void reset() throws IOException {
		out.reset();
	}

	public void close() throws IOException {
		out.close();
	}

	public void writeBoolean(final boolean val) throws IOException {
		out.writeBoolean(val);
	}

	public void writeByte(final int val) throws IOException {
		out.writeByte(val);
	}

	public void writeShort(final int val) throws IOException {
		out.writeShort(val);
	}

	public void writeChar(final int val) throws IOException {
		out.writeChar(val);
	}

	public void writeInt(final int val) throws IOException {
		out.writeInt(val);
	}

	public void writeLong(final long val) throws IOException {
		out.writeLong(val);
	}

	public void writeFloat(final float val) throws IOException {
		out.writeFloat(val);
	}

	public void writeDouble(final double val) throws IOException {
		out.writeDouble(val);
	}

	public void writeBytes(final String str) throws IOException {
		out.writeBytes(str);
	}

	public void writeChars(final String str) throws IOException {
		out.writeChars(str);
	}

	public void writeUTF(final String str) throws IOException {
		out.writeUTF(str);
	}

}
