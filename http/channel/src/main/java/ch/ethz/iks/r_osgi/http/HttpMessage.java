package ch.ethz.iks.r_osgi.http;

import java.io.DataInput;

public abstract class HttpMessage {

	public static HttpMessage getMessage(DataInput input) {
		
		return null;
	}
	
	public abstract byte[] getContent();
	
}
