package ch.ethz.iks.r_osgi.channels;

import ch.ethz.iks.r_osgi.RemoteOSGiException;
import ch.ethz.iks.r_osgi.RemoteServiceReference;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.types.Timestamp;

public interface ChannelEndpointManager {

	public void addRedundantEndpoint(URI service, URI redundant);

	public void removeRedundantEndpoint(URI service, URI redundant);

	public void setEndpointPolicy(URI service, int policy);

	public URI getLocalAddress();
	
	/**
	 * transform a timestamp into the peer's local time.
	 * 
	 * @param sender
	 *            the sender serviceURL.
	 * @param timestamp
	 *            the Timestamp.
	 * @return the transformed timestamp.
	 * @throws RemoteOSGiException
	 *             if the transformation fails.
	 * @since 0.2
	 */
	Timestamp transformTimestamp(Timestamp timestamp)
	throws RemoteOSGiException;
	
}
