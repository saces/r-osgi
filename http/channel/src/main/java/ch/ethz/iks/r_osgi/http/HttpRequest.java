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
			final ObjectOutputStream out) throws IOException {
		outStream.flush();
		final byte[] content = outStream.toByteArray();
		System.out.println("sending content of " + content.length + " bytes");
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
		buffer.append("Connection: keep-alive\r\n");
		buffer.append("Keep-Alive: 60000\r\n");
		for (Enumeration keys = headerpairs.keys(); keys.hasMoreElements();) {
			final String key = (String) keys.nextElement();
			buffer.append(key);
			buffer.append(": ");
			buffer.append(headerpairs.get(key));
			buffer.append("\r\n");
		}
		buffer.append("\r\n");

		byte bytes[];
		if (method == 1 && content != null) {
			int len = buffer.length();
			bytes = new byte[len + content.length];
			System.arraycopy(buffer.toString().getBytes(), 0, bytes, 0, len);
			System.arraycopy(content, 0, bytes, len, content.length);
		} else {
			bytes = buffer.toString().getBytes();
		}

		out.write(bytes);
		out.flush();
	}

}
