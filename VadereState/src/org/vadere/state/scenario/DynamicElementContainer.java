package org.vadere.state.scenario;

import java.awt.geom.RectangularShape;
import java.util.*;

import org.vadere.util.geometry.LinkedCellsGrid;
import org.vadere.util.geometry.shapes.VPoint;

public class DynamicElementContainer<T extends DynamicElement> {
	private transient final List<DynamicElementAddListener<T>> addListener;
	private transient final List<DynamicElementRemoveListener<T>> removeListener;
	private transient final List<DynamicElementMoveListener<T>> moveListener;

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
		this.moveListener = new LinkedList<>();
	}

	public synchronized LinkedCellsGrid<T> getCellsElements() {
		return cellsElements;
	}

	public synchronized Collection<T> getElements() {
		return elementMap.values();
	}

	public synchronized T getElement(int id) {
		return elementMap.get(id);
	}

	public void addInitialElement(T initialElement) {
		this.initialElements.add(initialElement);
	}

	public List<T> getInitialElements() {
		return this.initialElements;
	}

	public synchronized void addElement(T element) {
		this.elementMap.put(element.getId(), element);
		this.cellsElements.addObject(element);

		assert (elementMap.size() == cellsElements.size())
				: "Number of pedestrians in LinkedCellGrid does not match number of pedestrians" +
				" in topography";

		for (DynamicElementAddListener<T> listener : addListener) {
			listener.elementAdded(element);
		}
	}

	public synchronized void moveElement(T element, VPoint oldPosition) {
		this.cellsElements.moveObject(element, oldPosition);

		assert (elementMap.size() == cellsElements.size())
				: "Number of pedestrians in LinkedCellGrid does not match number of pedestrians" +
				" in topography";

		for (DynamicElementMoveListener<T> listener : moveListener) {
			listener.elementMove(element);
		}
	}

	public synchronized void removeElement(T element) {
		this.elementMap.remove(element.getId());
		this.cellsElements.removeObject(element);

		assert (elementMap.size() == cellsElements.size())
				: "Number of pedestrians in LinkedCellGrid does not match number of pedestrians" +
				" in topography";
		for (DynamicElementRemoveListener<T> listener : removeListener) {
			listener.elementRemoved(element);
		}
	}

	public synchronized void addElementRemovedListener(DynamicElementRemoveListener<T> listener) {
		this.removeListener.add(listener);
	}

	public synchronized void addElementAddedListener(DynamicElementAddListener<T> listener) {
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

	public boolean idExists(int id){
		return elementMap.containsKey(id);
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
