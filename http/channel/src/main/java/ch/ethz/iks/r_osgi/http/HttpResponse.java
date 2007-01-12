package ch.ethz.iks.r_osgi.http;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.ProtocolException;
import java.util.Hashtable;

public class HttpResponse {

	private double version;

	private int status;

	private Hashtable headerpairs = new Hashtable();

	private ByteArrayInputStream inStream;

	protected HttpResponse(final DataInputStream in) throws IOException {
		StringBuffer buffer = new StringBuffer();
		char c;
		while ((c = in.readChar()) != '\n') {
			buffer.append(c);
		}
		final String startline = buffer.toString();
		// HTTP/1.1 200 OK
		if (!startline.startsWith("HTTP/")) {
			System.out.println();
			System.out.println(startline);
			System.out.println();
			throw new ProtocolException("Parse error: Not a HTTP message");
		}
		int pos = startline.indexOf(" ");
		version = Double.parseDouble(startline.substring(5, pos));
		if (version < 1.0) {
			throw new ProtocolException(
					"Pre-HTTP/1.0 message are not supported.");
		}
		int pos2 = startline.indexOf(" ", pos + 1);
		status = Integer.parseInt(startline.substring(pos + 1, pos2));
		System.out.println("STATUS: " + status);
		System.out.println();
		System.out.println("HEADERS: ");
		String line;
		while (!"".equals(line = in.readLine())) {
			pos = line.indexOf(":");
			System.out.println(line);
			headerpairs.put(line.substring(0, pos), line.substring(pos + 1));
		}
		System.out.println();

		System.out.println("STILL " + in.available() + " BYTES AVAILABLE.");
		byte[] content = new byte[in.available()];
		in.readFully(content);
		inStream = new ByteArrayInputStream(content);
	}

	public double getVersion() {
		return version;
	}

	public int getStatus() {
		return status;
	}

	public String getHeader(String key) {
		return (String) headerpairs.get(key);
	}

	public String[] getHeaderKeys() {
		return (String[]) headerpairs.keySet().toArray(
				new String[headerpairs.size()]);
	}

	public ObjectInputStream getInputStream() throws IOException {
		return new ObjectInputStream(inStream);
	}

}
