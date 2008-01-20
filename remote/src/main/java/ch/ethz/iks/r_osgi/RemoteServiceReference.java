package ch.ethz.iks.r_osgi;

import java.io.Serializable;
import ch.ethz.iks.r_osgi.URI;

public interface RemoteServiceReference extends Serializable {

	static final long serialVersionUID = 1L;

	public String[] getServiceInterfaces();
		
	public URI getURI();
	
	public Object getProperty(String key);

	public String[] getPropertyKeys();
	
	public boolean isActive();
	
}
