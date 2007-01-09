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

}