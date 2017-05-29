package org.vadere.util.geometry;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.vadere.util.geometry.shapes.VPoint;

/**
 * A grid augmenting the position of generic objects, for faster access. O(1)
 * instead of O(n) for one fixed radius check. See
 * {@link LinkedCellsGrid#getObjects(java.awt.geometry.shapes.VPoint, double)}.
 * 
 * 
 */
public class LinkedCellsGrid<T> implements Iterable<T> {
	/**
	 * Key value pair holding an object with its assigned position.
	 * 
	 * 
	 */
	private class ObjectWithPosition<U> {
		U object;
		VPoint position;

		public ObjectWithPosition(U object, VPoint pos) {
			this.object = object;
			this.position = pos;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((object == null) ? 0 : object.hashCode());
			result = prime * result
					+ ((position == null) ? 0 : position.hashCode());
			return result;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			ObjectWithPosition<U> other = (ObjectWithPosition<U>) obj;
			if (object == null) {
				if (other.object != null) {
					return false;
				}
			} else if (!object.equals(other.object)) {
				return false;
			}
			if (position == null) {
				if (other.position != null) {
					return false;
				}
			} else if (!position.equals(other.position)) {
				return false;
			}
			return true;
		}
	}

	final private double left;
	final private double top;
	final private double width;
	final private double height;
	private GridCell<T>[][] grid;
	private List<ObjectWithPosition<T>> totalObjects = new LinkedList<ObjectWithPosition<T>>();
	private int[] gridSize = new int[2];
	private double[] cellSize = new double[2];

	/**
	 * One cell in the grid. It triangleContains a mapping from points to lists of
	 * objects. This means that one can store multiple objects in one cell.
	 * 
	 * 
	 * @param <E>
	 *        type of objects stored in this cell.
	 */
	private class GridCell<E> {
		public Map<VPoint, List<E>> objects = new HashMap<VPoint, List<E>>();

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((objects == null) ? 0 : objects.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof GridCell)) {
				return false;
			}
			GridCell other = (GridCell) obj;
			if (objects == null) {
				if (other.objects != null) {
					return false;
				}
			} else if (!objects.equals(other.objects)) {
				return false;
			}
			return true;
		}

		private LinkedCellsGrid getOuterType() {
			return LinkedCellsGrid.this;
		}
	}

	/**
	 * Generates an empty grid of GridCell&lt;T&gt; objects.
	 * 
	 * @param s
	 *        grid side length
	 * @return A two-dimensional grid of size "s^2" containing GridCell&lt;T&gt;
	 *         objects.
	 */
	@SuppressWarnings("unchecked")
	private GridCell<T>[][] generateGrid(int... s) {
		// Use Array native method to create array of a type only known at run
		// time
		this.grid = (GridCell<T>[][]) Array.newInstance(GridCell.class, s);
		for (int r = 0; r < grid.length; r++) {
			// TODO [priority=medium] [task=test] changed this [20.08.2014] here 1 to r - pls check this
			for (int c = 0; c < grid[r].length; c++) {
				grid[r][c] = new GridCell<T>();
			}
		}

		return this.grid;
	}

	/**
	 * Generates a LinkedCellsGrid with given dimension, position and number of
	 * items on one side.
	 * 
	 * @param left
	 *        x-position of top left corner
	 * @param top
	 *        y-position of top left corner
	 * @param width
	 *        width of the grid, in world units (e.g. [m])
	 * @param height
	 *        height of the grid, in world units (e.g. [m])
	 * @param sideLength
	 *        number of items on one side. the total number of grid cells
	 *        equals sideLength^2
	 */
	public LinkedCellsGrid(double left, double top, double width,
			double height, double sideLength) {
		this.left = left;
		this.top = top;
		this.width = width;
		this.height = height;


		// create grid
		/*
		 * this.gridSize = (int) Math.bound(1,
		 * Math.ceil(Math.bound(width, height) / sideLength));
		 * this.cellSize = Math.bound(width, height) / gridSize;
		 * this.grid = generateGrid(gridSize, gridSize);
		 */

		this.gridSize[0] = (int) Math.max(1, this.width / sideLength);
		this.gridSize[1] = (int) Math.max(1, this.height / sideLength);
		this.cellSize[0] = this.width / gridSize[0];
		this.cellSize[1] = this.height / gridSize[1];

		this.grid = generateGrid(this.gridSize[0], this.gridSize[1]);


	}

	/**
	 * Determines the discrete grid position (x,y) given a point with double
	 * coordinates.
	 * 
	 * @param pos
	 *        a given position with coordinate values of type double
	 * @return the position in the grid, from 0 to this.gridSize-1 in both
	 *         coordinates.
	 */
	private int[] gridPos(VPoint pos) {
		// compute position in the grid
		int iX = (int) Math.max(
				0,
				Math.min(this.gridSize[0] - 1,
						Math.floor((pos.x - left) / width * this.gridSize[0])));
		int iY = (int) Math.max(
				0,
				Math.min(this.gridSize[1] - 1,
						Math.floor((pos.y - top) / height * this.gridSize[1])));

		return new int[] {iX, iY};
	}

	/**
	 * Adds a given object to the grid at the given position. The position is
	 * discretized automatically to fit in the cells.
	 * 
	 * @param object
	 *        object to add
	 * @param pos
	 *        position in the grid
	 */
	public void addObject(final T object, final VPoint pos) {
		int[] gridPos = gridPos(pos);

		// store object in the grid cell.
		// if there is nothing there yet, create the list.
		if (!this.grid[gridPos[0]][gridPos[1]].objects.containsKey(pos)) {
			this.grid[gridPos[0]][gridPos[1]].objects.put(pos,
					new LinkedList<T>());
		}
		List<T> objects = this.grid[gridPos[0]][gridPos[1]].objects.get(pos);
		// add the object to the list stored in this cell
		objects.add(object);

		// also store it in the total objects list for easy iteration over all
		// stored objects.
		totalObjects.add(new ObjectWithPosition<T>(object, pos));
	}

	/**
	 * Returns a set of objects in the ball around pos with given radius.
	 * 
	 * @param pos
	 *        position of the center of the ball
	 * @param radius
	 *        radius of the ball
	 * @return set of objects, or an empty set if no objects are present.
	 */
	public List<T> getObjects(final VPoint pos, final double radius) {
		final List<T> result = new LinkedList<T>();

		int[] gridPos = gridPos(pos);
		int[] discreteRad = new int[2];
		discreteRad[0] = (int) Math.ceil(radius / this.cellSize[0]);
		discreteRad[1] = (int) Math.ceil(radius / this.cellSize[1]);

		final int maxRow = Math.min(this.gridSize[0] - 1, gridPos[0] + discreteRad[0]);
		final int maxCol = Math.min(this.gridSize[1] - 1, gridPos[1] + discreteRad[1]);

		for (int row = Math.max(0, gridPos[0] - discreteRad[0]); row <= maxRow; row++) {
			for (int col = Math.max(0, gridPos[1] - discreteRad[1]); col <= maxCol; col++) {

				for (Entry<VPoint, List<T>> entry : this.grid[row][col].objects
						.entrySet()) {
					// if the given position is closer than the radius, add all objects stored there
					if (entry.getKey().distance(pos) < radius) {
						result.addAll(entry.getValue());
					}
				}
			}
		}
		return result;
	}

	/**
	 * Removes the objects equal to the given object from the grid regardless of
	 * their position. Note that this function has complexity O(N), with N =
	 * number of objects in the grid.
	 * 
	 * @param object
	 */
	public void removeObject(T object) {
		for (Iterator<ObjectWithPosition<T>> iter = totalObjects.iterator(); iter
				.hasNext();) {
			ObjectWithPosition<T> obj = iter.next();
			if (obj.object.equals(object)) {
				iter.remove();

				// get the list of objects stored at the given position and
				// remove the object.
				VPoint pos = obj.position;
				int[] gridPos = gridPos(pos);
				List<T> objectsAtPos = this.grid[gridPos[0]][gridPos[1]].objects
						.get(pos);
				objectsAtPos.remove(object);

				// if the list is empty, remove the entry at this position
				if (objectsAtPos.isEmpty()) {
					this.grid[gridPos[0]][gridPos[1]].objects.remove(pos);
				}
			}
		}
	}

	/**
	 * Removes all objects.
	 */
	public void clear() {
		totalObjects.clear();
		this.grid = generateGrid(gridSize[0], gridSize[1]);
	}

	/**
	 * Provides an iterator over the objects stored in the grid.
	 */
	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private Iterator<ObjectWithPosition<T>> objectsIter = totalObjects
					.iterator();

			@Override
			public boolean hasNext() {
				return objectsIter.hasNext();
			}

			@Override
			public T next() {
				// only return the object itself, not the position
				ObjectWithPosition<T> obj = objectsIter.next();
				if (obj == null) {
					return null;
				} else {
					return obj.object;
				}
			}

			@Override
			public void remove() {
				objectsIter.remove();
			}
		};
	}

	/**
	 * Returns the size (number of different keys &lt;T&gt;) of List.
	 * 
	 * @return the size (number of different keys &lt;T&gt;) of List
	 */
	public int size() {
		return totalObjects.size();
	}

	/**
	 * Tests whether the linked cells grid triangleContains an object that equals(the
	 * given object). The complexity of this operation is O(N), N = number of
	 * objects in the grid.
	 * 
	 * @param element
	 * @return
	 */
	public boolean contains(final T element) {
		for (Iterator<T> iter = this.iterator(); iter.hasNext();) {
			if (iter.next().equals(element)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = java.lang.Double.doubleToLongBits(cellSize[0]);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + Arrays.hashCode(grid);
		result = prime * result + gridSize[1];
		temp = java.lang.Double.doubleToLongBits(height);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = java.lang.Double.doubleToLongBits(left);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = java.lang.Double.doubleToLongBits(top);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ ((totalObjects == null) ? 0 : totalObjects.hashCode());
		temp = java.lang.Double.doubleToLongBits(width);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof LinkedCellsGrid)) {
			return false;
		}
		LinkedCellsGrid other = (LinkedCellsGrid) obj;
		if (java.lang.Double.doubleToLongBits(cellSize[0]) != java.lang.Double
				.doubleToLongBits(other.cellSize[0])) {
			return false;
		}
		if (!Arrays.deepEquals(grid, other.grid)) {
			return false;
		}
		if (!Arrays.equals(gridSize, other.gridSize)) {
			return false;
		}
		if (java.lang.Double.doubleToLongBits(height) != java.lang.Double
				.doubleToLongBits(other.height)) {
			return false;
		}
		if (java.lang.Double.doubleToLongBits(left) != java.lang.Double
				.doubleToLongBits(other.left)) {
			return false;
		}
		if (java.lang.Double.doubleToLongBits(top) != java.lang.Double
				.doubleToLongBits(other.top)) {
			return false;
		}
		if (totalObjects == null) {
			if (other.totalObjects != null) {
				return false;
			}
		} else if (!totalObjects.equals(other.totalObjects)) {
			return false;
		}
		if (java.lang.Double.doubleToLongBits(width) != java.lang.Double
				.doubleToLongBits(other.width)) {
			return false;
		}
		return true;
	}

}
