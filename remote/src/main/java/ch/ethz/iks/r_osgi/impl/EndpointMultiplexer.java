package ch.ethz.iks.r_osgi.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.osgi.framework.ServiceRegistration;
import ch.ethz.iks.r_osgi.URI;
import ch.ethz.iks.r_osgi.RemoteOSGiException;
import ch.ethz.iks.r_osgi.RemoteOSGiMessage;
import ch.ethz.iks.r_osgi.channels.ChannelEndpoint;

class EndpointMultiplexer implements ChannelEndpoint {

	final static int NONE = 0;

	final static int FAILOVER_REDUNDANCY = 1;

	final static int LOADBALANCING_ANY = 2;

	final static int LOADBALANCING_ONE = 3;

	private ChannelEndpoint primary;

	private HashMap policies = new HashMap(0);

	private ServiceRegistration reg;

	private Map mappings = new HashMap();

	EndpointMultiplexer(final ChannelEndpoint primary) {
		this.primary = primary;
	}

	public void setPolicy(final URI service, int policy) {
		policies.put(service.toString(), new Integer(policy));
	}

	void addEndpoint(URI service, URI redundantService, ChannelEndpoint endpoint) {
		System.err.println("for service " + service
				+ " adding redundant service " + redundantService + " through "
				+ endpoint);
		Mapping mapping = (Mapping) mappings.get(service);
		if (mapping == null) {
			mapping = new Mapping(service.toString());
			mappings.put(service.toString(), mapping);
		}
		mapping.addRedundant(redundantService.toString(), endpoint);
	}

	void removeEndpoint(URI service, URI redundantService,
			ChannelEndpoint endpoint) {
		final Mapping mapping = (Mapping) mappings.get(service.toString());
		mapping.removeRedundant(endpoint);
		if (mapping.isEmpty()) {
			mappings.remove(service);
		}
	}

	public void dispose() {
		throw new IllegalArgumentException(
				"Not supported through endpoint multiplexer");
	}

	public Dictionary getPresentationProperties(String serviceURL) {
		return primary.getPresentationProperties(serviceURL);
	}

	public Dictionary getProperties(String serviceURL) {
		return primary.getProperties(serviceURL);
	}

	public URI getRemoteEndpoint() {
		return primary.getRemoteEndpoint();
	}

	public Object invokeMethod(String serviceURI, String methodSignature,
			Object[] args) throws Throwable {
		System.out.println("MULTIPLEXER: invoke " + serviceURI + " - "
				+ methodSignature);
		final Mapping mapping = (Mapping) mappings.get(serviceURI);
		if (mapping == null) {
			return primary.invokeMethod(serviceURI, methodSignature, args);
		} else {
			final Integer p = (Integer) policies.get(serviceURI);
			if (p == null) {
				return primary.invokeMethod(mapping.getMapped(primary),
						methodSignature, args);
			} else {
				final int policy = p.intValue();
				if (policy == LOADBALANCING_ANY) {
					final ChannelEndpoint endpoint = mapping.getAny();
					return endpoint.invokeMethod(mapping.getMapped(endpoint),
							methodSignature, args);
				} else {
					try {
						if (!primary.isConnected()) {
							throw new RemoteOSGiException("channel went down");
						}
						System.out.println("primary is " + primary);
						System.out.println("mapping is "
								+ mapping.getMapped(primary));
						return primary.invokeMethod(mapping.getMapped(primary),
								methodSignature, args);
					} catch (RemoteOSGiException e) {
						if (policy == FAILOVER_REDUNDANCY) {
							// do the failover
							final ChannelEndpoint next = mapping.getNext();
							if (next != null) {
								primary.untrackRegistration(serviceURI);
								primary = next;
								primary.trackRegistration(serviceURI, reg);
								System.err.println("DOING FAILOVER TO "
										+ primary.getRemoteEndpoint());
								return primary.invokeMethod(mapping
										.getMapped(primary), methodSignature,
										args);
							}
						}
						throw e;
					}
				}
			}
		}
	}

	public void receivedMessage(RemoteOSGiMessage msg) {
		throw new IllegalArgumentException(
				"Not supported through endpoint multiplexer");
	}

	public void trackRegistration(final String service,
			final ServiceRegistration reg) {
		this.reg = reg;
		primary.trackRegistration(service, reg);
	}

	public void untrackRegistration(final String service) {
		primary.untrackRegistration(service);
	}

	public boolean isConnected() {
		return true;
	}

	private class Mapping {

		private String serviceURI;
		private Random random = new Random(System.currentTimeMillis());
		private List redundant = new ArrayList(0);
		private Map uriMapping = new HashMap(0);

		private Mapping(final String serviceURI) {
			this.serviceURI = serviceURI;
			uriMapping.put(primary, serviceURI);
		}

		private ChannelEndpoint getOne() {
			return primary;
		}

		private void addRedundant(final String redundantServiceURI,
				final ChannelEndpoint endpoint) {
			redundant.add(endpoint);
			uriMapping.put(endpoint, redundantServiceURI);
		}

		private void removeRedundant(final ChannelEndpoint endpoint) {
			redundant.remove(endpoint);
			uriMapping.remove(endpoint);
		}

		private String getMapped(final ChannelEndpoint endpoint) {
			//System.out.println("REQUESTED " + endpoint);
			//System.out.println("HAVE " + uriMapping);
			return (String) uriMapping.get(endpoint);
		}

		private ChannelEndpoint getNext() {
			return (ChannelEndpoint) redundant.remove(0);
		}

		private boolean isEmpty() {
			return redundant.size() == 0;
		}

		private ChannelEndpoint getAny() {
			return (ChannelEndpoint) redundant.get(random.nextInt(redundant
					.size()));
		}

	}

}
