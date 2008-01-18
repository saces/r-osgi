package ch.ethz.iks.r_osgi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class URI implements Serializable {

	private String scheme;

	private InetAddress host;

	private int port;

	private String fragment;

	public URI(final String uriString) throws UnknownHostException {
		parse(uriString);
	}

	public static URI create(final String uriString) {
		try {
			return new URI(uriString);
		} catch (Throwable t) {
			return null;
		}
	}

	public static void main(String[] args) throws Exception {
		URI uri1 = new URI("http://localhost:8080");
		URI uri2 = new URI("r-osgi://flowsgi.inf.ethz.ch:9278#32");
		System.out.println();
		System.out.println(uri1);
		System.out.println(uri1.getScheme());
		System.out.println(uri1.getHostName());
		System.out.println(uri1.getPort());
		System.out.println(uri1.getFragment());
		System.out.println();
		System.out.println(uri2);
		System.out.println(uri2.getScheme());
		System.out.println(uri2.getHostName());
		System.out.println(uri2.getPort());
		System.out.println(uri2.getFragment());
		System.out.println();
		System.out.println(uri1.resolve("#55"));
		System.out.println();
		System.out.println(uri1.equals("http://127.0.0.1:8080"));
		URI uri3 = new URI("http://127.0.0.1:8080");
		System.out.println(uri3.equals(uri1));
		System.out.println(uri3.hashCode());
		System.out.println(uri1.hashCode());
	}

	private void parse(final String uriString) throws UnknownHostException {
		int cs = 0;
		int ce = uriString.length();
		final int p1 = uriString.indexOf("://");
		if (p1 > -1) {
			scheme = uriString.substring(0, p1);
			cs = p1 + 3;
		}
		final int p2 = uriString.lastIndexOf("#");
		if (p2 > -1) {
			fragment = uriString.substring(p2 + 1);
			ce = p2;
		}
		final int p3 = uriString.indexOf(":", cs);
		if (p3 > -1) {
			port = Integer.parseInt(uriString.substring(p3 + 1, ce));
			ce = p3;
		}
		host = InetAddress.getByName(uriString.substring(cs, ce));
	}

	public String getScheme() {
		return scheme;
	}

	public InetAddress getHost() {
		return host;
	}

	public String getHostName() {
		return host.getHostName();
	}

	public int getPort() {
		return port;
	}

	public String getFragment() {
		return fragment;
	}

	public URI resolve(String add) {
		return URI.create(toString() + add);
	}

	public int hashCode() {
		return scheme.hashCode() + host.hashCode() + port + (fragment != null ? fragment.hashCode() : 0);
	}

	public String toString() {
		return scheme + "://" + getHostName() + ":" + port
				+ (fragment == null ? "" : "#" + fragment);
	}

	public boolean equals(final Object other) {
		if (other instanceof String) {
			return equals(URI.create((String) other));
		} else if (other instanceof URI) {
			final URI otherURI = (URI) other;
			return scheme.equals(otherURI.scheme)
					&& host.equals(otherURI.host)
					&& port == otherURI.port
					&& ((fragment == null && otherURI.fragment == null) || fragment != null && fragment
							.equals(otherURI.fragment));
		} else {
			return false;
		}
	}

	private void writeObject(final ObjectOutputStream out) throws IOException {
		out.writeUTF(toString());
	}

	private void readObject(final ObjectInputStream in) throws IOException {
		parse(in.readUTF());
	}
}
