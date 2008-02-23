package ch.ethz.iks.r_osgi.test.service.impl;

import ch.ethz.iks.r_osgi.test.service.ServiceInterfaceOne;
import ch.ethz.iks.r_osgi.test.service.ServiceInterfaceThree;
import ch.ethz.iks.r_osgi.test.service.ServiceInterfaceTwo;

public class MultipleInterfaceServiceImpl implements ServiceInterfaceOne, ServiceInterfaceTwo, ServiceInterfaceThree {

	public String callOne(String s) {
		return "ONE";
	}

	public String callTwo(String s) {
		return "TWO";
	}

	public String callThree(String s) {
		return "THREE";
	}

}
