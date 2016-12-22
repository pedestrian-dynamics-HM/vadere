package org.vadere.state.scenario;

import java.awt.geom.RectangularShape;
import java.util.*;

import org.vadere.util.geometry.LinkedCellsGrid;

public class DynamicElementContainer<T extends DynamicElement> {
	private transient final List<DynamicElementAddListener<T>> addListener;
	private transient final List<DynamicElementRemoveListener<T>> removeListener;

	private final Map<Integer, T> elementMap;

	private final List<T> initialElements;

	/**
	 * LinkedCellsGrid storing all elements for fast access.
	 */
	private final LinkedCellsGrid<T> cellsElements;

	private final RectangularShape bounds;
	private final double cellSize;

	public DynamicElementContainer(RectangularShape bounds, double cellSize) {
		elementMap = new HashMap<>();
		initialElements = new LinkedList<>();
		this.bounds = bounds;
		this.cellSize = cellSize;

		this.cellsElements = new LinkedCellsGrid<>(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(),
				bounds.getHeight(), cellSize);

		this.addListener = new LinkedList<>();
		this.removeListener = new LinkedList<>();
	}

	public LinkedCellsGrid<T> getCellsElements() {
		return cellsElements;
	}

	public Collection<T> getElements() {
		return elementMap.values();
	}

	public T getElement(int id) {
		return elementMap.get(id);
	}

	public void addInitialElement(T initialElement) {
		this.initialElements.add(initialElement);
	}

	public List<T> getInitialElements() {
		return this.initialElements;
	}

	public void addElement(T element) {
		this.elementMap.put(element.getId(), element);
		this.cellsElements.addObject(element, element.getPosition());

		for (DynamicElementAddListener<T> listener : addListener) {
			listener.elementAdded(element);
		}
	}

	public void removeElement(T element) {
		this.elementMap.remove(element.getId());
		this.cellsElements.removeObject(element);

		for (DynamicElementRemoveListener<T> listener : removeListener) {
			listener.elementRemoved(element);
		}
	}

	public void addElementRemovedListener(DynamicElementRemoveListener<T> listener) {
		this.removeListener.add(listener);
	}

	public void addElementAddedListener(DynamicElementAddListener<T> listener) {
		this.addListener.add(listener);
	}

	public List<DynamicElementAddListener<T>> getElementAddedListener() {
		return this.addListener;
	}

	public List<DynamicElementRemoveListener<T>> getElementRemovedListener() {
		return this.removeListener;
	}

	public void clear() {
		this.elementMap.clear();
		this.cellsElements.clear();
	}

	public void clearListeners() {
		this.addListener.clear();
		this.removeListener.clear();
	}

	@Override
	protected DynamicElementContainer<T> clone() throws CloneNotSupportedException {
		DynamicElementContainer<T> clone = new DynamicElementContainer<>(bounds, cellSize);

		for (T element : this.elementMap.values()) {
			clone.addElement(element);
		}
		for (T initialElement : this.initialElements) {
			clone.addInitialElement(initialElement);
		}

		// clone listener?

		return clone;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((cellsElements == null) ? 0 : cellsElements.hashCode());
		result = prime * result
				+ ((elementMap == null) ? 0 : elementMap.hashCode());
		result = prime * result
				+ ((initialElements == null) ? 0 : initialElements.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof DynamicElementContainer))
			return false;
		DynamicElementContainer other = (DynamicElementContainer) obj;
		if (cellsElements == null) {
			if (other.cellsElements != null)
				return false;
		} else if (!cellsElements.equals(other.cellsElements))
			return false;
		if (elementMap == null) {
			if (other.elementMap != null)
				return false;
		} else if (!elementMap.equals(other.elementMap))
			return false;
		if (initialElements == null) {
			if (other.initialElements != null)
				return false;
		} else if (!initialElements.equals(other.initialElements))
			return false;
		return true;
	}

}
