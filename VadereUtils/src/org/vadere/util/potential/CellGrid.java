package org.vadere.util.potential;

import java.awt.Point;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.bridj.util.Tuple;
import org.vadere.util.data.Tupel;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.MathUtil;

/**
 * Grid consisting of regular arranged sampling points an a plane, each storing
 * a value of type 'T'. A Grid has a certain width and height. The resolution
 * represents the distance of two grid points along the x- or y-axis. The values
 * of the grid points can be regarded as elements of a matrix. Each element in
 * the matrix refers to a certain point in the grid. Internally the values are
 * stored as array of values in column major order.
 */
public class CellGrid {

	/** Width of the grid. */
	protected final double width;
	/** Height of the grid. */
	protected final double height;
	/** Distance of point along x- and y-axis */
	protected final double resolution;
	/** Number of points along x axis. */
	protected final int numPointsX;
	/** Number of points along y axis. */
	protected final int numPointsY;

	protected CellState[][] values;

	/**
	 * Creates an grid with the given width, height and resolution. All grid
	 * point values are initialized with 'value'.
	 */
	public CellGrid(double width, double height, double resolution,
			CellState value) {
		this.width = width;
		this.height = height;
		this.resolution = resolution;

		/* 0.001 avoids that numPointsX/Y are too small due to numerical errors. */
		numPointsX = (int) Math.floor(width / resolution + 0.001) + 1;
		numPointsY = (int) Math.floor(height / resolution + 0.001) + 1;

		values = new CellState[numPointsX][numPointsY];

		reset(value);
	}

	/**
	 * Creates a deep copy of the given grid.
	 */
	public CellGrid(CellGrid grid) {
		width = grid.width;
		height = grid.height;
		resolution = grid.resolution;
		numPointsX = grid.numPointsX;
		numPointsY = grid.numPointsY;
		values = new CellState[numPointsX][numPointsY];

		for (int row = 0; row < numPointsY; row++) {
			for (int col = 0; col < numPointsX; col++) {
				values[col][row] = grid.values[col][row].clone();
			}
		}
	}

	/** Returns the values of all data points. */
	public Iterable<CellState> getRawBuffer() {
		return new Iterable<CellState>() {
			@Override
			public Iterator<CellState> iterator() {
				return new Iterator<CellState>() {
					private int row = 0;
					private int col = 0;

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}

					@Override
					public CellState next() {
						CellState result = values[col][row];
						col++;
						if (col >= numPointsX) {
							col = 0;
							row++;
						}
						return result;
					}

					@Override
					public boolean hasNext() {
						return col < numPointsX && row < numPointsY;
					}
				};
			}
		};
	}

	/** Returns the width of the grid. */
	public double getWidth() {
		return width;
	}

	/**
	 * Returns the resolution of the grid, which is the distance of two grid
	 * points along x- or y-axis.
	 */
	public double getResolution() {
		return resolution;
	}

	/** Returns the height of the grid. */
	public double getHeight() {
		return height;
	}

	/** Returns the number of grid points along the x-axis. */
	public int getNumPointsX() {
		return numPointsX;
	}

	/** Returns the number of grid points along the y-axis. */
	public int getNumPointsY() {
		return numPointsY;
	}

	/**
	 * Converts the matrix indices to coordinates.
	 */
	public VPoint pointToCoord(int pointX, int pointY) {
		return new VPoint(pointX * resolution, pointY * resolution);
	}

	/**
	 * Converts the matrix indices to coordinates.
	 */
	public VPoint pointToCoord(Point p) {
		return pointToCoord(p.x, p.y);
	}

	/**
	 * Returns the value of the grid point specified by matrix element indices.
	 */
	public CellState getValue(int pointX, int pointY) {
		return values[pointX][pointY];
	}

	/**
	 * Returns the value of the grid point specified by the given matrix element
	 * index 'p'.
	 */
	public CellState getValue(Point p) {
		return getValue(p.x, p.y);
	}

	/**
	 * Sets the value of the grid point specified by matrix element indices.
	 */
	public void setValue(int pointX, int pointY, CellState value) {
		// points.set(pointToIdx(pointX, pointY), value);
		values[pointX][pointY] = value;
	}

	/**
	 * Sets the value of the grid point specified by the matrix element index
	 * 'p'.
	 */
	public void setValue(Point p, CellState value) {
		setValue(p.x, p.y, value);
	}

	/**
	 * Returns the closest grid point (matrix index) to the given coordinates.
	 */
	public Point getNearestPoint(double x, double y) {
		if (x < 0) {
			x = 0;
		}
		if (y < 0) {
			y = 0;
		}
		if (y > getHeight()) {
			y = getHeight();
		}
		if (x > getWidth()) {
			x = getWidth();
		}
		return new Point((int) (x / resolution + 0.5),
				(int) (y / resolution + 0.5));
	}

	/**
	 * Returns the closest grid point (matrix index) to the given coordinates
	 * towards origin.
	 */
	public Point getNearestPointTowardsOrigin(double x, double y) {
		if (x < 0) {
			x = 0;
		}
		if (y < 0) {
			y = 0;
		}
		if (y > getHeight()) {
			y = getHeight();
		}
		if (x > getWidth()) {
			x = getWidth();
		}
		return new Point((int) (x / resolution), (int) (y / resolution));
	}

	/**
	 * Returns the closest grid point (matrix index) to the given coordinates
	 * towards origin.
	 */
	public Point getNearestPointTowardsOrigin(VPoint p) {
		return getNearestPointTowardsOrigin(p.x, p.y);
	}

	public Point getNearestPointTowardsOrigin(Point p) {
		return getNearestPointTowardsOrigin(p.x, p.y);
	}

	/**
	 * Resturns the distance of grid points specified by its matrix indices.
	 */
	public double pointDistance(int pointX1, int pointY1, int pointX2,
			int pointY2) {
		return Math.sqrt(Math.pow(pointY2 - pointY1, 2)
				+ Math.pow(pointX2 - pointX1, 2))
				* resolution;
	}

	/**
	 * Resturns the distance of grid points specified by its matrix indices.
	 */
	public double pointDistance(Point p1, Point p2) {
		return pointDistance(p1.x, p1.y, p2.x, p2.y);
	}

	/** Sets the values of all grid points to 'value'. */
	public void reset(CellState value) {
		for (int row = 0; row < numPointsY; row++) {
			for (int col = 0; col < numPointsX; col++) {
				values[col][row] = value.clone();
			}
		}
	}

	/** Dumps the grid values. */
	public void dump() {
		for (int y = 0; y < numPointsY; ++y) {
			for (int x = 0; x < numPointsX; ++x) {
				System.out.print(getValue(x, y).toString() + " ");
			}
			System.out.print("\n");
		}
	}

	/** Returns a copy of the grid. See copy constructor for more information. */
	@Override
	public Object clone() {
		return new CellGrid(this);
	}

	public List<Point> getLegitNeumannNeighborhood(Point point) {
		return MathUtil.getNeumannNeighborhood(point).stream().filter(p -> isValidPoint(p))
				.collect(Collectors.toList());
	}

	public List<Point> getLegitMooreNeighborhood(Point point) {
		return MathUtil.getMooreNeighborhood(point).stream().filter(p -> isValidPoint(p)).collect(Collectors.toList());
	}

	public Stream<Point> pointStream() {
		return IntStream.range(0, getNumPointsX())
				.mapToObj(x -> IntStream.range(0, getNumPointsY()).mapToObj(y -> new Point(x, y)))
				.flatMap(stream -> stream);
	}

	public boolean isValidPoint(Point point) {
		Point p = (point);

		if ((p.x < 0) || (p.x >= numPointsX)) {
			return false;
		}

		if ((p.y < 0) || (p.y >= numPointsY)) {
			return false;
		}
		return true;
	}
}
