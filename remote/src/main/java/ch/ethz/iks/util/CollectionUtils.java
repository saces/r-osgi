/* Copyright (c) 2006-2007 Jan S. Rellermeyer
 * Information and Communication Systems Research Group (IKS),
 * Department of Computer Science, ETH Zurich.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *    - Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    - Neither the name of ETH Zurich nor the names of its contributors may be
 *      used to endorse or promote products derived from this software without
 *      specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package ch.ethz.iks.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Collection utilities.
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 * @since 0.1
 */
public final class CollectionUtils {

	/**
	 * hidden default constructor.
	 */
	private CollectionUtils() {

	}

	/**
	 * add a unique value to a collection. If the value already exists, it will
	 * return <code>false</code>.
	 * 
	 * @param collection
	 *            the collection.
	 * @param value
	 *            the value.
	 * @return boolean.
	 * @since 0.1
	 */
	public static boolean addUnique(final Collection collection,
			final Object value) {
		if (collection.contains(value)) {
			return false;
		}
		return collection.add(value);
	}

	/**
	 * add a collection of values to a collection.
	 * 
	 * @param collection
	 *            the collection.
	 * @param values
	 *            the values.
	 * @return boolean.
	 * @since 0.1
	 */
	public static boolean addAllUnique(final Collection collection,
			final Collection values) {
		boolean changed = false;
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			changed = addUnique(collection, iter.next());
		}
		return changed;
	}

	/**
	 * the intersection of two collections.
	 * 
	 * @param c1
	 *            collection1.
	 * @param c2
	 *            collection2.
	 * @return the intersection, i.e. all elements that are in both collections.
	 * @since 0.1
	 */
	public static Collection intersection(final Collection c1,
			final Collection c2) {
		List list = new ArrayList();
		for (Iterator iter = c1.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (c2.contains(obj)) {
				list.add(obj);
			}
		}
		return list;
	}

	/**
	 * add a value to a value list in a Map.
	 * 
	 * @param map
	 *            the map.
	 * @param key
	 *            the key.
	 * @param value
	 *            the value to be added to the list.
	 * @since 0.1
	 */
	public static void addValue(final Map map, final Object key,
			final Object value) {
		List values;

		if ((values = (List) map.get(key)) == null) {
			values = new ArrayList();
		}

		values.add(value);
		map.put(key, values);
	}

	/**
	 * add a value to a value list in a Map that has a limited capacity. If the
	 * capacity is exceeded, the oldest entry is discarded.
	 * 
	 * @param map
	 *            the map.
	 * @param key
	 *            the key.
	 * @param value
	 *            the value to be added.
	 * @param maxEntries
	 *            the capacity of the value list.
	 * @since 0.6
	 */
	public static void addValue(final Map map, final Object key,
			final Object value, final int maxEntries) {
		List values;

		if ((values = (List) map.get(key)) == null) {
			values = new ArrayList();
		}

		values.add(value);
		while (values.size() > maxEntries) {
			values.remove(0);
		}
		map.put(key, values);
	}

	/**
	 * remove a value from a list in a Map.
	 * 
	 * @param map
	 *            the map.
	 * @param keys
	 *            the keys that are affected.
	 * @param value
	 *            the value to be deleted in the lists.
	 * @since 0.1
	 */
	public static void removeValue(final Map map, final Object[] keys,
			final Object value) {
		List values;
		for (int i = 0; i < keys.length; i++) {
			if ((values = (List) map.get(keys[i])) == null) {
				continue;
			}
			values.remove(value);

			if (values.isEmpty()) {
				map.remove(keys[i]);
			} else {
				map.put(keys[i], values);
			}
		}
	}
}