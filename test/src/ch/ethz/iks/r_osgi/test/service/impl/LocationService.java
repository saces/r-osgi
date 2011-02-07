package ch.ethz.iks.r_osgi.test.service.impl;

import ch.ethz.iks.r_osgi.test.exported.AccessPoint;
import ch.ethz.iks.r_osgi.test.service.ILocationService;

public class LocationService implements ILocationService {

	public void AddLocation(String LocationName, AccessPoint[] accessPoints) {
		// TODO Auto-generated method stub

	}

	public String GetLocation(AccessPoint[] accessPoints) {
		// TODO Auto-generated method stub
		return null;
	}

	public String GetMessage(final String message) {
		// TODO Auto-generated method stub
		return message;
	}

	public String Test(final AccessPoint p) {
		return "TEST";
	}

}
