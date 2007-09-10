package ch.ethz.iks.r_osgi.sample.service;

import ch.ethz.iks.r_osgi.sample.api.ServiceInterface;

/**
 * just a simple sample service plays around with strings
 */
public final class ServiceImpl implements ServiceInterface {

	/**
	 * echo service, echos <code>count</code> times the message.
	 */
	public String echoService(final String message, final Integer count) {
		StringBuffer buffer = new StringBuffer();
		final int c = count.intValue();
		for (int i = 0; i < c; i++) {
			buffer.append(message);
			if (i < c - 1) {
				buffer.append(" | ");
			}
		}
		return buffer.toString();
	}

	/**
	 * reverse service, returns the reversed message.
	 */
	public String reverseService(String message) {
		return new StringBuffer().append(message).reverse().toString();
	}

	public void local() {
		System.out.println("Server: local called");
		throw new RuntimeException("Local cannot be called remotely");
	}

	public void zero() {
		System.out.println("Server: zero called.");
	}

	public boolean equalsRemote(Object other) {
		return equals(other);
	}

	public void printRemote(int i, float f) {
		System.out.println("i is " + i);
		System.out.println("f is " + f);
	}

	public boolean verifyBlock(byte[] data, int i, int j, int k) {
		System.out.println("GOT CALLED WITH " + new String(data));
		return true;
	}
}