package org.vadere.util.data.cellgrid;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.math.InterpolationUtil;
import org.vadere.util.math.MathUtil;

import java.awt.*;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

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

	protected final double xMin;

	protected final double yMin;

	protected CellState[][] values;

	/**
	 * Creates an grid with the given width, height and resolution. All grid
	 * point values are initialized with 'value'.
	 */
	public CellGrid(double width, double height, double resolution,
	                CellState value, double xMin, double yMin) {
		this.width = width;
		this.height = height;
		this.resolution = resolution;
		this.xMin = xMin;
		this.yMin = yMin;

		/* 0.001 avoids that numPointsX/Y are too small due to numerical errors. */
		numPointsX = (int) Math.floor(width / resolution + 0.001) + 1;
		numPointsY = (int) Math.floor(height / resolution + 0.001) + 1;

		values = new CellState[numPointsX][numPointsY];

		reset(value);
	}

	/**
	 * Creates an grid with the given width, height and resolution. All grid
	 * point values are initialized with 'value'.
	 */
	public CellGrid(double width, double height, double resolution, CellState value) {
		this(width, height, resolution, value, 0, 0);
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
		xMin = grid.xMin;
		yMin = grid.yMin;

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

	/**
	 * Load data for CellState[][] structure from the given table. Method asumes the dimension
	 * of the table matches the dimension of {@link #values}
	 *
	 * @param table contains data in the form [x, y, (potential)value, tag]
	 */
	public void loadFromTable(Table table){
		for (Row r : table){
			values[r.getInt("x")][r.getInt("y")].potential = r.getDouble("value");
			values[r.getInt("x")][r.getInt("y")].tag =
					PathFindingTag.valueOf(r.getString("tag"));
		}
	}

	/**
	 * Generate table view of {@link #values} to save as cache
	 * *
	 * @return table representation of {@link #values} data in the from [x, y, (potential)value, tag]
	 */
	public Table asTable(){
		int len = numPointsX * numPointsY;
		IntColumn colX = IntColumn.create("x", new int[len]);
		IntColumn colY = IntColumn.create("y", new int[len]);
		DoubleColumn colVal = DoubleColumn.create("value", new double[len]);
		StringColumn colTag = StringColumn.create("tag", new String[len]);
		int tblRow = 0;
		for (int row = 0; row < numPointsY; row++) {
			for (int col = 0; col < numPointsX; col++) {
				colX.set(tblRow, col);
				colY.set(tblRow, row);
				colVal.set(tblRow, values[col][row].potential);
				colTag.set(tblRow, values[col][row].tag.name());
				tblRow++;
			}
		}
		return Table.create("floorfield").addColumns(colX, colY, colVal, colTag);
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
		return new VPoint(xMin + pointX * resolution, yMin + pointY * resolution);
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
		if (x < xMin) {
			x = xMin;
		}
		if (y < yMin) {
			y = yMin;
		}
		if (y > getHeight() + yMin) {
			y = getHeight() + yMin;
		}
		if (x > getWidth() + xMin) {
			x = getWidth() + xMin;
		}
		return new Point((int) ((x - xMin) / resolution + 0.5),
				(int) ((y - yMin) / resolution + 0.5));
	}

	/**
	 * Returns the distance of grid points specified by its matrix indices.
	 */
	public double pointDistance(int pointX1, int pointY1, int pointX2,
	                            int pointY2) {
		return Math.sqrt(Math.pow(pointY2 - pointY1, 2)
				+ Math.pow(pointX2 - pointX1, 2))
				* resolution;
	}

	/**
	 * Returns the distance of grid points specified by its matrix indices.
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
	public CellGrid clone() {
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

	public double getMinX() { return xMin; }

	public double getMinY() { return yMin; }

	public double getMaxX () {
		return  xMin + width;
	}

	public double getMaxY() {
		return yMin + height;
	}

	public boolean isValidPoint(Point point) {
		return isValidPoint(point.x, point.y);
	}

	public boolean isValidPoint(final int x, final int y) {

		if ((x < 0) || (x >= numPointsX)) {
			return false;
		}

		if ((y < 0) || (y >= numPointsY)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns a function VPoint (x,y-coordinate) -> Double (potential) which
	 * computes the bilinearInterpolated potential for a given coordinate.
	 *
	 * @return  a function VPoint (x,y-coordinate) -> Double (potential)
	 */
	public Function<IPoint, Double> getInterpolationFunction() {
		return pos -> {
			int incX = 1;
			int incY = 1;

			Point gridPoint = getNearestPoint(pos.getX(), pos.getY());

			if (gridPoint.x + 1 >= getNumPointsX()) {
				incX = 0;
			}

			if (gridPoint.y + 1 >= getNumPointsY()) {
				incY = 0;
			}


			VPoint gridPointCoord = pointToCoord(gridPoint);

			double z1 = getValue(gridPoint).potential;
			double z2 = getValue(gridPoint.x + incX, gridPoint.y).potential;
			double z3 = getValue(gridPoint.x + incX, gridPoint.y + incY).potential;
			double z4 = getValue(gridPoint.x, gridPoint.y + incY).potential;

			double t = (pos.getX() - gridPointCoord.x) / getResolution();
			double u = (pos.getY() - gridPointCoord.y) / getResolution();

			return InterpolationUtil.bilinearInterpolation(z1, z2, z3, z4, t, u);
		};
	}

	public Pair<Double, Double> getInterpolatedValueAt(final double x, final double y) {
		Point gridPoint = getNearestPoint(x, y);
		VPoint gridPointCoord = pointToCoord(gridPoint);
		int incX = 1, incY = 1;
		double gridPotentials[] = new double[4];

		if (gridPoint.x + 1 >= getNumPointsX()) {
			incX = 0;
		}

		if (gridPoint.y + 1 >= getNumPointsY()) {
			incY = 0;
		}


		gridPotentials[0] = getValue(gridPoint).potential;
		gridPotentials[1] = getValue(gridPoint.x + incX, gridPoint.y).potential;
		gridPotentials[2] = getValue(gridPoint.x + incX, gridPoint.y + incY).potential;
		gridPotentials[3] = getValue(gridPoint.x, gridPoint.y + incY).potential;


		/* Interpolate the known (potential < Double.MAX_VALUE) values. */
		Pair<Double, Double> result = InterpolationUtil.bilinearInterpolationWithUnkown(
				gridPotentials,
				(x - gridPointCoord.x) / getResolution(),
				(y - gridPointCoord.y) / getResolution());

		return result;
	}

	public Pair<Double, Double> getInterpolatedValueAt(@NotNull final IPoint pos) {
		return getInterpolatedValueAt(pos.getX(), pos.getY());
	}
}
