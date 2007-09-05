package ch.ethz.iks.r_osgi;

import java.io.Serializable;
import java.net.URI;

public interface RemoteServiceReference extends Serializable {

	public String[] getServiceInterfaces();
		
	public URI getURI();
	
	public Object getProperty(String key);

	public String[] getPropertyKeys();
	
}
