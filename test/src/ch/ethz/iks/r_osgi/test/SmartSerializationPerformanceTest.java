package ch.ethz.iks.r_osgi.test;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import ch.ethz.iks.r_osgi.test.exported.NonSerializableClass;
import ch.ethz.iks.r_osgi.test.exported.SerializableClass;
import ch.ethz.iks.util.SmartObjectOutputStream;
import junit.framework.TestCase;

public class SmartSerializationPerformanceTest extends TestCase {

	private ByteArrayOutputStream bout1;
	private ByteArrayOutputStream bout2;
	private ObjectOutputStream oout1;
	private ObjectOutputStream oout2;

	protected void setUp() throws Exception {
		bout1 = new ByteArrayOutputStream();
		bout2 = new ByteArrayOutputStream();

		oout1 = new SmartObjectOutputStream(bout1);
		oout2 = new ObjectOutputStream(bout2);
	}

	protected void tearDown() throws Exception {
		oout1.close();
		oout2.close();
	}

	public void testInteger() throws Exception {
		System.out.println("Integer");
		long time1 = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			oout1.writeObject(new Integer(i));
		}
		time1 = System.currentTimeMillis() - time1;

		System.out.println(time1 + " ms, " + bout1.toByteArray().length);

		long time2 = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			oout2.writeObject(new Integer(i));
		}
		time2 = System.currentTimeMillis() - time2;

		System.out.println(time2 + " ms, " + bout2.toByteArray().length);
		oout1.flush();
		oout2.flush();
		assertTrue(bout1.toByteArray().length < bout2.toByteArray().length);
	}

	public void testLong() throws Exception {
		System.out.println("Long");
		long time1 = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			oout1.writeObject(new Long(i));
		}
		time1 = System.currentTimeMillis() - time1;

		System.out.println(time1 + " ms, " + bout1.toByteArray().length);

		long time2 = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			oout2.writeObject(new Long(i));
		}
		time2 = System.currentTimeMillis() - time2;

		System.out.println(time2 + " ms, " + bout2.toByteArray().length);

		oout1.flush();
		oout2.flush();
		assertTrue(bout1.toByteArray().length < bout2.toByteArray().length);
	}

	public void testBoolean() throws Exception {
		System.out.println("Boolean");
		long time1 = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			oout1.writeObject(new Boolean(i % 2 == 0));
		}
		time1 = System.currentTimeMillis() - time1;

		System.out.println(time1 + " ms, " + bout1.toByteArray().length);

		long time2 = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			oout2.writeObject(new Boolean(i % 2 == 0));
		}
		time2 = System.currentTimeMillis() - time2;

		System.out.println(time2 + " ms, " + bout2.toByteArray().length);

		oout1.flush();
		oout2.flush();
		assertTrue(bout1.toByteArray().length < bout2.toByteArray().length);
	}

	public void testString() throws Exception {
		System.out.println("String");
		long time1 = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			oout1.writeObject(String.valueOf(i));
		}
		time1 = System.currentTimeMillis() - time1;

		System.out.println(time1 + " ms, " + bout1.toByteArray().length);

		long time2 = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			oout2.writeObject(String.valueOf(i));
		}
		time2 = System.currentTimeMillis() - time2;

		System.out.println(time2 + " ms, " + bout2.toByteArray().length);

		oout1.flush();
		oout2.flush();
		assertTrue(bout1.toByteArray().length < bout2.toByteArray().length);
	}

	public void testString2() throws Exception {
		System.out.println("String2");
		long time1 = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			oout1.writeObject(String.valueOf(i % 100));
		}
		time1 = System.currentTimeMillis() - time1;

		System.out.println(time1 + " ms, " + bout1.toByteArray().length);

		long time2 = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			oout2.writeObject(String.valueOf(i % 100));
		}
		time2 = System.currentTimeMillis() - time2;

		System.out.println(time2 + " ms, " + bout2.toByteArray().length);

		oout1.flush();
		oout2.flush();
		assertTrue(bout1.toByteArray().length < bout2.toByteArray().length);
	}

	public void testSerializableClass() throws Exception {
		System.out.println("SerializableClass");
		long time1 = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			oout1.writeObject(new SerializableClass(new Integer(i)));
		}
		time1 = System.currentTimeMillis() - time1;

		System.out.println(time1 + " ms, " + bout1.toByteArray().length);

		long time2 = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			oout2.writeObject(new SerializableClass(new Integer(i)));
		}
		time2 = System.currentTimeMillis() - time2;

		System.out.println(time2 + " ms, " + bout2.toByteArray().length);

		oout1.flush();
		oout2.flush();
		assertTrue(bout1.toByteArray().length < bout2.toByteArray().length);
	}

	public void testNonSerializableClass() throws Exception {
		System.out.println("NonSerializableClass");
		long time1 = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			oout1.writeObject(new NonSerializableClass(new Integer(i)));
		}
		time1 = System.currentTimeMillis() - time1;

		System.out.println(time1 + " ms, " + bout1.toByteArray().length);

		long time2 = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			oout2.writeObject(new SerializableClass(new Integer(i)));
		}
		time2 = System.currentTimeMillis() - time2;

		System.out.println(time2 + " ms, " + bout2.toByteArray().length);

		oout1.flush();
		oout2.flush();
		assertTrue(bout1.toByteArray().length < bout2.toByteArray().length);
	}

	public void testNonSerizlizableArray() throws Exception {
		System.out.println("NonSerializableClassArray");
		long time1 = System.currentTimeMillis();
		for (int i = 0; i < 10000; i++) {
			oout1.writeObject(new NonSerializableClass[] {
					new NonSerializableClass(new Integer(i)),
					new NonSerializableClass(new Integer(1)),
					new NonSerializableClass("FOO") });
		}
		time1 = System.currentTimeMillis() - time1;

		System.out.println(time1 + " ms, " + bout1.toByteArray().length);

		long time2 = System.currentTimeMillis();
		for (int i = 0; i < 10000; i++) {
			oout2.writeObject(new SerializableClass[] {
					new SerializableClass(new Integer(i)),
					new SerializableClass(new Integer(1)),
					new SerializableClass("FOO") });
		}
		time2 = System.currentTimeMillis() - time2;

		System.out.println(time2 + " ms, " + bout2.toByteArray().length);

		oout1.flush();
		oout2.flush();
		assertTrue(bout1.toByteArray().length < bout2.toByteArray().length);
	}
}
