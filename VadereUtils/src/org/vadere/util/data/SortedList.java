package org.vadere.util.data;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Comparator;

public class SortedList<E> extends AbstractList<E> {

	private ArrayList<E> internalList = new ArrayList<>();
	private Comparator<E> comparator;

	public SortedList(final Comparator<E> comparator) {
		this.comparator = comparator;
	}

	@Override
	public void add(int position, E e) {
		if (internalList.isEmpty()) {
			internalList.add(e);
		} else {
			internalList.add(findPrecessor(e), e);
		}
	}

	@Override
	public E get(int i) {
		return internalList.get(i);
	}

	@Override
	public int size() {
		return internalList.size();
	}

	@Override
	public boolean remove(final Object o) {
		return internalList.remove(o);
	}

	@Override
	public E remove(final int index) {
		return internalList.remove(index);
	}

	@Override
	public void clear() {
		internalList.clear();
	}

	/**
	 * Returns the position the element would be inserted.
	 * 
	 * @param e the element
	 * @return the position
	 */
	public int findPrecessor(final E e) {
		int i = 0;
		while (i < internalList.size() && comparator.compare(internalList.get(i), e) < 0) {
			i++;
		}
		return i;
	}
}
