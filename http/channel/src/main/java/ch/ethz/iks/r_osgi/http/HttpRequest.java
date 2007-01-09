package ch.ethz.iks.r_osgi.http;

import java.io.DataInput;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;

public class HttpRequest extends HttpMessage {

	public static final short GET = 0;

	public static final short POST = 1;

	private static final String[] methods = { "GET", "POST" };

	private String requestURI;

	private Hashtable headerpairs = new Hashtable();

	private byte[] content;

	public HttpRequest(String requestURI) {
		this.requestURI = requestURI;
	}

	protected HttpRequest(final String initline, final DataInput input) {
		
		
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

	public void setContent(byte[] bytes) {
		content = bytes;
	}

	public byte[] getBytes(final String host, final short method) throws UnsupportedEncodingException {
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
		
		byte[] bytes;
		if (method == POST && content != null) {	
			final int len = buffer.length();
			bytes = new byte[len + content.length];
			System.arraycopy(buffer.toString().getBytes(), 0, bytes, 0, len);
			System.arraycopy(content, 0, bytes, len, content.length);
		} else {
			bytes = buffer.toString().getBytes();
		}

		return bytes;
	}

	public byte[] getContent() {
		return content;
	}
}
