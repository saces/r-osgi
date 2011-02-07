package ch.ethz.iks.r_osgi.test.service;

import ch.ethz.iks.r_osgi.test.exported.AccessPoint;

public interface ILocationService {
	public void AddLocation(String LocationName, AccessPoint[] accessPoints);

	public String GetLocation(AccessPoint[] accessPoints);

	public String GetMessage(String Message);

	public String Test(AccessPoint p);

}
