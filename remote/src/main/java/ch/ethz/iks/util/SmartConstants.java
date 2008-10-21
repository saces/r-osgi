package ch.ethz.iks.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class SmartConstants {

	/**
	 * the positive list contains class names of classes that are
	 * string-serializable.
	 */
	static List positiveList = new ArrayList(Arrays.asList(new Object[] {
			"java.lang.Integer", "java.lang.Boolean",
			"java.lang.Long", "java.lang.Short", "java.lang.Byte" })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 

	static HashSet blackList = new HashSet();
	static {
		blackList.add("org.osgi.framework.ServiceReference");
		blackList.add("org.osgi.framework.ServiceRegistration");
	}

	static final HashMap idToClass = new HashMap();
	static final HashMap classToId = new HashMap();
	static {
		idToClass.put("I", Integer.class); //$NON-NLS-1$
		classToId.put(Integer.class.getName(), "I"); //$NON-NLS-1$
		idToClass.put("Z", Boolean.class); //$NON-NLS-1$
		classToId.put(Boolean.class.getName(), "Z"); //$NON-NLS-1$
		idToClass.put("J", Long.class); //$NON-NLS-1$
		classToId.put(Long.class.getName(), "J"); //$NON-NLS-1$
		idToClass.put("S", Short.class); //$NON-NLS-1$
		classToId.put(Short.class.getName(), "S"); //$NON-NLS-1$
		idToClass.put("B", Byte.class); //$NON-NLS-1$
		classToId.put(Byte.class.getName(), "B"); //$NON-NLS-1$
		idToClass.put("C", Character.class); //$NON-NLS-1$
		classToId.put(Character.class.getName(), "C"); //$NON-NLS-1$
		idToClass.put("D", Double.class); //$NON-NLS-1$
		classToId.put(Double.class.getName(), "D"); //$NON-NLS-1$
		idToClass.put("F", Float.class); //$NON-NLS-1$
		classToId.put(Float.class.getName(), "F"); //$NON-NLS-1$
	}
}
