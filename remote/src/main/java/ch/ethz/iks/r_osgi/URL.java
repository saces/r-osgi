package ch.ethz.iks.r_osgi;

import java.net.InetAddress;

public final class URL {

	private static final short PROTOCOL = 0;

	private static final short HOST = 1;

	private static final short PORT = 2;

	private static final short SERVICE_ID = 3;

	public static String getURL(final String protocol, final String host,
			final int port) {
		return get(protocol, host, String.valueOf(port), null);
	}

	public static String getURL(final String protocol, final String host,
			final int port, final long serviceID) {
		return get(protocol, host, String.valueOf(port), String
				.valueOf(serviceID));
	}

	public static String getURL(final String protocol, final InetAddress host,
			final int port) {
		return getURL(protocol, host.getHostName(), port);
	}

	public static String getURL(final String protocol, final InetAddress host,
			final int port, final long serviceID) {
		return getURL(protocol, host.getHostName(), port, serviceID);
	}

	private static String get(final String protocol, final String host,
			final String port, final String serviceID) {
		return serviceID == null ? protocol + "://" + host + ":" + port
				: protocol + "://" + host + ":" + port + "/" + serviceID;
	}

	public static String getProtocol(final String url) {
		return parse(url)[PROTOCOL];
	}

	public static String getHost(final String url) {
		return parse(url)[HOST];
	}

	public static int getPort(final String url) {
		return Integer.parseInt(parse(url)[PORT]);
	}

	public static Long getServiceID(final String url) {
		final String id = parse(url)[SERVICE_ID];
		return id == null ? null : Long.decode(id);
	}

	public static String rewrite(final String url, final String protocol,
			final String host, final String port, final String serviceID) {
		final String[] result = parse(url);
		if (protocol != null) {
			result[PROTOCOL] = protocol;
		}
		if (host != null) {
			result[HOST] = host;
		}
		if (port != null) {
			result[PORT] = port;
		}
		if (serviceID != null) {
			result[SERVICE_ID] = serviceID;
		}
		return get(result[PROTOCOL], result[HOST], result[PORT],
				result[SERVICE_ID]);
	}

	private static String[] parse(final String url) {
		final String[] result = new String[4];
		final int pos1 = url.indexOf("://");
		if (pos1 == -1) {
			throw new IllegalArgumentException("Malformed URL " + url);
		}
		result[0] = url.substring(0, pos1);
		final String rest1 = url.substring(pos1 + 3);
		final int pos2 = rest1.indexOf(":");
		if (pos2 == -1) {
			throw new IllegalArgumentException("Malformed URL " + url);
		}
		result[1] = rest1.substring(0, pos2);
		final String rest2 = rest1.substring(pos2 + 1);
		final int pos3 = rest2.indexOf("/");
		if (pos3 == -1) {
			result[2] = rest2;
		} else {
			result[2] = rest2.substring(0, pos3);
			result[3] = rest2.substring(pos3 + 1);
		}
		return result;
	}

	public static void main(String[] args) {
		final String url = "r-osgi://localhost:9000/23";
		final String url2 = "r-osgi+managed://flowsgi.inf.ethz.ch:9278";

		System.out.println(java.util.Arrays.asList(parse(url)));
		System.out.println(java.util.Arrays.asList(parse(url2)));
	}
}
