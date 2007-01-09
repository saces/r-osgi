package ch.ethz.iks.r_osgi.http;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.Hashtable;

public class HttpResponse extends HttpMessage {

	private double version;

	private int status;

	private Hashtable headerpairs = new Hashtable();

	private byte[] content;
	
	private DataInputStream stream;

	protected HttpResponse(final String startline, final DataInputStream in)
			throws IOException {
		// HTTP/1.1 200 OK
		if (!startline.startsWith("HTTP/")) {
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

		String line;
		while (!"".equals(line = in.readLine())) {
			pos = line.indexOf(":");
			headerpairs.put(line.substring(0, pos), line.substring(pos + 1));
		}

		stream = in;
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

	public byte[] getContent() {
		return content;
	}
	
	public DataInputStream getStream() {
		return stream;
	}
}
