package org.vadere.util.io;

import java.util.Collection;
import java.util.LinkedList;

public class ListUtils {

	/**
	 * Select objects from a list that have the given class type.
	 * 
	 * @param lists
	 * @param type
	 * @return a list containing only the
	 */
	public static <T, S extends T> LinkedList<S> select(Collection<T> list,
			Class<S> type) {
		LinkedList<S> result = new LinkedList<>();
		for (T element : list) {
			if (element.getClass().isAssignableFrom(type)) {
				result.add(type.cast(element));
			}
		}
		return result;
	}

}
