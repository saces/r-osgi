package ch.ethz.iks.r_osgi;

import java.io.Serializable;

public interface RemoteServiceReference extends Serializable {

	public String[] getServiceInterfaces();
		
	public String getURL();
	
	public Object getProperty(String key);

	public String[] getPropertyKeys();
	
}
