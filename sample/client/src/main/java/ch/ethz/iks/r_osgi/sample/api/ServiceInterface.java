package ch.ethz.iks.r_osgi.sample.api;

/**
 * the interface of the simple sample service.
 */
public interface ServiceInterface {

	public String echoService(final String message, final Integer count);

	public String reverseService(final String message);

	public boolean equalsRemote(Object other);

	public void printRemote(int i, float f);

	public void zero();

	public void local();

	public boolean verifyBlock(final byte[] data, final int i, final int j,
			final int k);

	public String[] checkArray(String str, int i);

	public String[][] checkDoubleArray(String str, int i, int j);

	public byte[] echoByteArray1(final byte[] bytes);

	public byte[][] echoByteArray2(final byte[][] bytes);
}
