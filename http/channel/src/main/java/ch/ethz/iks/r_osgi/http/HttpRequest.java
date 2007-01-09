package ch.ethz.iks.r_osgi.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

public class HttpRequest {

	public static final short GET = 0;

	public static final short POST = 1;

	private static final String[] methods = { "GET", "POST" };

	private String requestURI;

	private Hashtable headerpairs = new Hashtable();

	private ByteArrayInputStream inStream;

	private ByteArrayOutputStream outStream;
	
	private byte[] content;

	public HttpRequest(String requestURI) {
		this.requestURI = requestURI;
	}

	public HttpRequest(DataInputStream in) throws Exception {
		// TODO: parse
		System.out.println("starting the parsing");
		in.readLine();

		String line;
		int pos;
		while (!"".equals(line = in.readLine())) {
			pos = line.indexOf(":");
			headerpairs.put(line.substring(0, pos), line.substring(pos + 1));
		}
		System.out.println("parsed the headers " + headerpairs);
		Integer len = (Integer) headerpairs.get("Content-Length");
		if (len != null) {
			System.out.println("expecting content of length " + len);
			content = new byte[len.intValue()];
			in.readFully(content);
			inStream = new ByteArrayInputStream(content);
		}
		System.out.println("finished parsing");
	}

	public void setHeader(String key, String value) {
		headerpairs.put(key, value);
	}

	public String[] getHeaderKeys() {
		return (String[]) headerpairs.keySet().toArray(
				new String[headerpairs.size()]);
	}

	public String getHeader(String key) {
		return (String) headerpairs.get(key);
	}

	public void removeHeader(String key) {
		headerpairs.remove(key);
	}

	public ObjectOutputStream getOutputStream() throws IOException {
		outStream = new ByteArrayOutputStream();
		return new ObjectOutputStream(outStream);
	}

	public ObjectInputStream getInputStream() throws IOException {
		return new ObjectInputStream(inStream);
	}
	
	public byte[] getContent() {
		return content;
	}

	public void send(final short method, final String host,
			final DataOutputStream out) throws IOException {

		final byte[] content = outStream.toByteArray();
		StringBuffer buffer = new StringBuffer();
		buffer.append(methods[method]);
		buffer.append(' ');
		buffer.append(requestURI);
		buffer.append(' ');
		buffer.append("HTTP/1.1");
		buffer.append("\r\n");
		buffer.append("Host: " + host + "\r\n");
		if (method == POST && content != null) {
			buffer.append("Content-Length: " + content.length + "\r\n");
		}
		for (Enumeration keys = headerpairs.keys(); keys.hasMoreElements();) {
			final String key = (String) keys.nextElement();
			buffer.append(key);
			buffer.append(": ");
			buffer.append(headerpairs.get(key));
			buffer.append("\r\n");
		}
		buffer.append("\r\n");

		if (content.length > 0) {
			buffer.append(content);
		}

		out.write(buffer.toString().getBytes());
	}

}
