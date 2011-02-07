package ch.ethz.iks.r_osgi.test.exported;

import java.lang.reflect.Array;

public class NonSerializableClass {

	Object state;

	public NonSerializableClass(final Object state) {
		this.state = state;
	}

	public NonSerializableClass() {

	}

	public boolean equals(final Object o) {
		if (o instanceof NonSerializableClass) {
			return deepEquals(state, ((NonSerializableClass) o).state);
		}
		return false;
	}

	private boolean deepEquals(final Object o1, final Object o2) {
		if (o1.getClass().isArray() && o2.getClass().isArray()) {
			if (Array.getLength(o1) != Array.getLength(o2)) {
				return false;
			}

			for (int i = 0; i < Array.getLength(o1); i++) {
				if (!deepEquals(Array.get(o1, i), Array.get(o2, i))) {
					return false;
				}
			}
			return true;
		}
		return o1.equals(o2);
	}

}
