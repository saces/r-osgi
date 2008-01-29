package ch.ethz.iks.r_osgi.test.service;

public interface ArgumentTestService {

	public void none();

	public void byte0(byte b);

	public void byte1(byte[] array);

	public void byte3(byte[][][] array);

	public byte byteObj0(Byte b);

	public byte[] byteObj1(Byte b);

	public byte[][][] byteObj3(Byte b);

	public void short0(short s);

	public void short1(short[] array);

	public void short3(short[][][] array);

	public short shortObj0(Short s);

	public short[] shortObj1(Short s);

	public short[][][] shortObj3(Short s);

	public void int0(int i);

	public void int1(int[] array);

	public void int3(int[][][] array);

	public int intObj0(int i);

	public int[] intObj1(int i);

	public int[][][] intObj3(int i);

}
