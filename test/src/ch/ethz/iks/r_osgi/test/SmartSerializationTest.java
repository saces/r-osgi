package ch.ethz.iks.r_osgi.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Array;
import java.util.Random;

import ch.ethz.iks.r_osgi.test.exported.NonSerializableClass;
import ch.ethz.iks.r_osgi.test.exported.SerializableClass;
import ch.ethz.iks.util.SmartObjectInputStream;
import ch.ethz.iks.util.SmartObjectOutputStream;
import junit.framework.TestCase;

public class SmartSerializationTest extends TestCase {

	private SmartObjectOutputStream out;
	private SmartObjectInputStream in;

	public SmartSerializationTest() {
		super("SmartSerializationTest");
	}

	protected void setUp() throws Exception {
		super.setUp();

		final PipedInputStream readin = new PipedInputStream();
		final PipedOutputStream readout = new PipedOutputStream(readin);

		final PipedOutputStream writeout = new PipedOutputStream();
		final PipedInputStream writein = new PipedInputStream(writeout);

		new PipeThread(writein, readout).start();

		out = new SmartObjectOutputStream(writeout);
		out.flush();
		in = new SmartObjectInputStream(readin);

		final Random ran = new Random();
		ran.nextBytes(BYTES);
	}

	protected void tearDown() throws Exception {
		super.tearDown();

		in.close();
		out.close();
	}

	public static final int INT = 1000;
	public static final float FLOAT = 1000F;
	public static final double DOUBLE = 102341.124122431;
	public static final char CHAR = 'a';
	public static final boolean BOOLEAN = true;
	public static final byte BYTE = 0x10;
	public static final long LONG = 1234735798523322344L;
	public static final byte[] BYTES = new byte[512];
	public static final String STRING = "34784sdfljklsdf34ujlksdjlkksdf";
	public static final SerializableClass S1 = new SerializableClass(STRING);
	public static final SerializableClass S11 = new SerializableClass(
			new String(BYTES));
	public static final SerializableClass S2 = new SerializableClass(
			new SerializableClass(new SerializableClass[] { S1, S11 }));
	public static final NonSerializableClass N1 = new NonSerializableClass(
			STRING);
	public static final NonSerializableClass N2 = new NonSerializableClass(
			new NonSerializableClass(new NonSerializableClass[] { N1, N1 }));
	public static final NonSerializableClass N3 = new NonSerializableClass(
			new SerializableClass(new NonSerializableClass(STRING)));

	public void testPrimitiveTypes() throws Exception {
		out.writeInt(INT);
		out.flush();
		assertTrue(in.readInt() == INT);

		out.writeFloat(FLOAT);
		out.flush();
		assertTrue(in.readFloat() == FLOAT);

		out.writeDouble(DOUBLE);
		out.flush();
		assertTrue(in.readDouble() == DOUBLE);

		out.writeChar(CHAR);
		out.flush();
		assertTrue(in.readChar() == CHAR);

		out.writeBoolean(BOOLEAN);
		out.flush();
		assertTrue(in.readBoolean() == BOOLEAN);

		out.writeByte(BYTE);
		out.flush();
		assertTrue(in.readByte() == BYTE);

		out.writeLong(LONG);
		out.flush();
		assertTrue(in.readLong() == LONG);

		final byte[] copy = new byte[BYTES.length];
		out.write(BYTES);
		out.flush();
		in.readFully(copy);
		assertBytesEqual(BYTES, copy);

		out.writeUTF(STRING);
		out.flush();
		assertEquals(STRING, in.readUTF());
	}

	public void testSimpleSerializable() throws Exception {
		out.writeObject(S1);
		out.flush();
		assertEquals(S1, in.readObject());
	}

	public void testNestedSerializable() throws Exception {
		out.writeObject(S2);
		out.flush();
		assertDeepEquals(S2, in.readObject());
	}

	public void testSimpleNonSerializable() throws Exception {
		out.writeObject(N1);
		out.flush();
		assertEquals(N1, in.readObject());
	}

	public void testNestedNonSerializable() throws Exception {
		out.writeObject(N2);
		out.flush();
		assertDeepEquals(N2, in.readObject());
	}

	public void testMixed() throws Exception {
		out.writeObject(N3);
		out.flush();
		assertDeepEquals(N3, in.readObject());
	}

	private void assertBytesEqual(byte[] b1, byte[] b2) {
		assertNotNull(b1);
		assertNotNull(b2);
		assertTrue(b1.length == b2.length);

		for (int i = 0; i < b1.length; i++) {
			assertTrue(b1[i] == b2[i]);
		}
	}

	private void assertDeepEquals(final Object o1, final Object o2) {
		assertNotNull(o1);
		assertNotNull(o2);

		if (o1.getClass().isArray() && o2.getClass().isArray()) {
			assertTrue(Array.getLength(o1) == Array.getLength(o2));

			for (int i = 0; i < Array.getLength(o1); i++) {
				assertDeepEquals(Array.get(o1, i), Array.get(o2, i));
			}
		}

		assertTrue(o1.equals(o2));
	}

	class PipeThread extends Thread {

		private final byte[] buffer = new byte[1024];
		private final InputStream in;
		private final OutputStream out;

		PipeThread(final InputStream in, final OutputStream out) {
			this.in = in;
			this.out = out;
		}

		public void run() {
			int read = 0;
			try {
				while (read > -1) {
					read = in.read(buffer);
					if (read > 0) {
						out.write(buffer, 0, read);
					}
				}
			} catch (final IOException ioe) {
				// ioe.printStackTrace();
			}
		}
	}

}
