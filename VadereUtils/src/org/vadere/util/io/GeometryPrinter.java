package org.vadere.util.io;

import java.util.LinkedList;
import java.util.List;

import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.Geometry;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

/**
 * Prints a {@link Geometry} or a double array in a MATLAB friendly format to
 * the console.
 * 
 */
public class GeometryPrinter {

	/**
	 * Print the given geometry in a MATLAB friendly format to the console.
	 * 
	 * @param toVisualize
	 * @param gridSideLen
	 *        Side length of the grid for MATLABs mesh(...) function.
	 * @param minX
	 *        minimum x value of the geometry
	 * @param minY
	 *        minimum y value of the geometry
	 * @param width
	 *        width of the geometry
	 * @param height
	 *        height of the geometry
	 */
	public static String geometry2string(Geometry toVisualize, int gridSideLen,
			double minX, double minY, double width, double height) {
		// units per grid side
		double gridBinSideLen = Math.sqrt(height * width) / gridSideLen;

		// initialize array
		StringBuilder resultStringBuilder = new StringBuilder("");

		// collect all polygons from the geometry
		List<VPolygon> polygons = new LinkedList<VPolygon>();
		polygons.addAll(toVisualize.getPolygons());

		// create grid, initially with zeros
		double[][] grid = new double[gridSideLen][gridSideLen];

		// draw 1.0 on each line
		for (VPolygon p : polygons) {
			List<VPoint> pointList = p.getPoints();
			if (pointList.isEmpty()) {
				continue;
			}

			// add first point at the end to get a closed loop
			pointList.add(pointList.get(0));

			// draw lines on grid
			for (int i = 0; i < pointList.size() - 1; i++) {
				VPoint p1 = pointList.get(i);
				VPoint p2 = pointList.get(i + 1);
				for (double f = 0.0; f < 1.0; f += 1.0 / (p1.distance(p2) / (0.5 * gridBinSideLen))) {
					VPoint currP = GeometryUtils.interpolate(p1, p2, f);
					int gridX = (int) ((currP.getX() - minX) / width * (gridSideLen - 1));
					int gridY = (int) ((currP.getY() - minY) / height * (gridSideLen - 1));
					if (gridX >= 0 && gridX < gridSideLen && gridY >= 0
							&& gridY < gridSideLen) {
						grid[gridX][gridY] = 1.0;
					}
				}
			}
		}

		// build string
		for (int r = 0; r < gridSideLen; r++) {
			for (int c = 0; c < gridSideLen; c++) {
				resultStringBuilder.append(grid[r][c]);
				if (c < gridSideLen - 1) {
					resultStringBuilder.append(",");
				} else if (r < gridSideLen - 1) {
					resultStringBuilder.append(System.lineSeparator());
				}
			}
		}

		// close array
		resultStringBuilder.append("");

		// return result
		return (resultStringBuilder.toString());
	}

	/**
	 * Prints a given double grid to a string.
	 * 
	 * @param grid
	 * @return
	 */
	public static String grid2string(double[][] grid) {

		StringBuilder sb = new StringBuilder("");

		for (int r = 0; r < grid.length; r++) {
			for (int c = 0; c < grid[0].length; c++) {
				sb.append(grid[r][c]);
				if (c < grid.length - 1) {
					sb.append(",");
				}
			}
			if (r < grid.length - 1) {
				sb.append("\n");
			}
		}
		sb.append("");
		return sb.toString();
	}

	/**
	 * Prints a given double grid to a string.
	 * 
	 * @param grid
	 * @return
	 */
	public static String grid2string(double[][][] grid) {

		StringBuilder sb = new StringBuilder("");

		for (int r = 0; r < grid.length; r++) {
			for (int c = 0; c < grid[0].length; c++) {
				for (int d = 0; d < grid[0][0].length; d++) {
					sb.append(grid[r][c][d]);
					if (d < grid.length - 1) {
						sb.append(",");
					}
				}
				sb.append(";");
			}
			if (r < grid.length - 1) {
				sb.append("\n");
			}
		}
		sb.append("");
		return sb.toString();
	}
}
