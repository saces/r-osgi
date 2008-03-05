package ch.ethz.iks.r_osgi.test.service.impl;

import ch.ethz.iks.r_osgi.test.service.ServiceInterfaceA;
import ch.ethz.iks.r_osgi.test.service.ServiceInterfaceB;
import ch.ethz.iks.r_osgi.test.service.ServiceInterfaceC;

public class MultipleInterfacesPlusInheritanceServiceImpl implements
		ServiceInterfaceA, ServiceInterfaceB, ServiceInterfaceC {

	public String fooA() {
		return "A";
	}

	public String fooB() {
		return "B";
	}

	public String fooC() {
		return "C";
	}

}
