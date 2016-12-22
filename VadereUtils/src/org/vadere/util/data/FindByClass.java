package org.vadere.util.data;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FindByClass {

	public static <T> T findSingleObjectOfClass(Collection<?> objects, Class<T> clazz)
			throws IllegalArgumentException {

		@SuppressWarnings("unchecked")
		List<T> result = objects.stream()
				.filter(o -> o.getClass() == clazz)
				.map(o -> (T) o)
				.collect(Collectors.toList());

		if (result.size() > 1) {
			throw new IllegalArgumentException("Object of class " + clazz + " exists multiple times.");
		}

		return result.isEmpty() ? null : result.get(0);
	}

	@SuppressWarnings("unchecked")
	public static <T> T findFirstObjectOfClass(Collection<?> objects, Class<T> clazz) {
		for (Object o : objects) {
			if (o.getClass() == clazz) {
				return (T) o;
			}
		}
		return null;
	}

}
