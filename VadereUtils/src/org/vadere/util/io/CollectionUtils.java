package org.vadere.util.io;

import com.google.common.collect.Iterables;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

public class CollectionUtils {

	/**
	 * Select objects from a list that have the given class type.
	 * 
	 * @param list
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

	/**
	 * Returns true iff the given Collections contain exactly the same elements with exactly the same cardinalities.
	 * That is, iff the cardinality of e in a is equal to the cardinality of e in b, for each element e in a or b.
	 * @param a
	 * @param b
	 * @param equator
	 * @param <T>
	 * @return
	 */
	public static <T> boolean isEqualCollection(final Collection<? extends T> a, final Collection<? extends T> b, final IEquator<T> equator) {
		Collection<EquatorWrapper<T>> ewA = a.stream().map(obj -> new EquatorWrapper<>(equator, obj)).collect(Collectors.toList());
		Collection<EquatorWrapper<T>> ewB = b.stream().map(obj -> new EquatorWrapper<>(equator, obj)).collect(Collectors.toList());
		return Iterables.elementsEqual(ewA, ewB);
	}

	public static <T> boolean isEqualSet(final Set<? extends T> a, final Set<? extends T> b, final IEquator<T> equator) {
		Set<EquatorWrapper<T>> ewA = a.stream().map(obj -> new EquatorWrapper<>(equator, obj)).collect(Collectors.toSet());
		Set<EquatorWrapper<T>> ewB = b.stream().map(obj -> new EquatorWrapper<>(equator, obj)).collect(Collectors.toSet());
		return ewA.equals(ewB);
	}

	public interface IEquator<T> {
		boolean equate(T a, T b);
		int hash(T a);
	}

	private static class EquatorWrapper<T> {
        private final IEquator<T> equator;
        private final T object;

        public EquatorWrapper(final IEquator<T> equator, final T object) {
            this.equator = equator;
            this.object = object;
        }

        public T getObject() {
            return object;
        }

		        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof EquatorWrapper)) {
	                return false;
            }

            @SuppressWarnings("unchecked")
            final EquatorWrapper<T> otherObj = (EquatorWrapper<T>) obj;
            return equator.equate(object, otherObj.getObject());
        }

		        @Override
        public int hashCode() {
            return equator.hash(object);
        }
    }


}
