package org.vadere.simulator.dataprocessing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

/**
 * Utility class for tests. Helps to generate random positions and special position on an hexagon
 * grid.
 *
 */
public class CreatePoints {

	private static Random random = new Random();

	/**
	 * Generates randomly numberOfPoints points in a rectangle.
	 * It is possible that this algorithm generate equal points.
	 *
	 * @param boundary the boundary the points will be contained in
	 * @param numberOfPoints the number of points that will be generated
	 * @return a array containing randomly generated points
	 */
	public static VPoint[] generateRandomVPoint(final VRectangle boundary, int numberOfPoints) {
		VPoint[] positions = new VPoint[numberOfPoints];
		double maxX = boundary.x + boundary.width;
		double maxY = boundary.y + boundary.height;

		double minX = boundary.x;
		double minY = boundary.y;

		double rangeX = boundary.width - boundary.x;
		double rangeY = boundary.height - boundary.y;

		for (int i = 0; i < numberOfPoints; i++) {
			double x = random.nextDouble() * rangeX + minX;
			double y = random.nextDouble() * rangeY + minY;
			positions[i] = new VPoint(x, y);
		}

		return positions;
	}

	/**
	 * Adds for each point in the list at most 6 new points around the point (hexagon pattern).
	 * The hexagonAmbitRadius decides how big these the hexagon is. If a point already in the
	 * list (with a distance tolerance of Utils.DOUBLE_EPS) it won't be added. You
	 * can call this method multiple times to construct centered hexagons. If you start with
	 * 1 Point you will receive 7 points than 19 and so on (see
	 * http://en.wikipedia.org/wiki/Centered_hexagonal_number).
	 * If a point is outside the bounds than it will not be added.
	 *
	 * @param points the list that will be filled with new points (arranged in a hexagon pattern)
	 * @param bounds the bounds all points contained in
	 * @param hexagonAmbitRadius the ambit radius of the hexagon pattern
	 * @return true, if there are at least one new point in the list, false otherwise
	 */
	public static boolean addHexagonPoints(final Collection<VPoint> points, final VRectangle bounds,
			final double hexagonAmbitRadius) {
		Collection<VPoint> copy = new ArrayList<>();
		copy.addAll(points);
		boolean hasAddPoint = false;
		for (VPoint cPoint : points) {
			boolean addPoint = addHexagonPoints(cPoint, copy, bounds, hexagonAmbitRadius);
			if (addPoint) {
				hasAddPoint = addPoint;
			}
		}
		points.clear();
		points.addAll(copy);
		return hasAddPoint;
	}


	private static boolean containsPoint(final Collection<VPoint> collection, final VPoint point, double tolerance) {
		for (VPoint cPoint : collection) {
			if (cPoint.equals(point, tolerance)) {
				return true;
			}
		}

		return false;
	}

	private static boolean addHexagonPoints(final VPoint center, final Collection<VPoint> points,
			final VRectangle bounds, final double hexagonAmbitRadius) {

		double hexagonInCircleRadius = hexagonAmbitRadius * Math.sqrt(3) / 2;


		boolean addPoint = false;
		double x = center.x - hexagonAmbitRadius;
		double y = center.y;
		VPoint left = new VPoint(x, y);
		if (!containsPoint(points, left, GeometryUtils.DOUBLE_EPS)
				&& bounds.contains(new VPoint(left.x - hexagonAmbitRadius, left.y))) {
			points.add(left);
			addPoint = true;
		}

		x = center.x + hexagonAmbitRadius;
		y = center.y;
		VPoint right = new VPoint(x, y);
		if (!containsPoint(points, right, GeometryUtils.DOUBLE_EPS)
				&& bounds.contains(new VPoint(right.x + hexagonAmbitRadius, right.y))) {
			points.add(right);
			addPoint = true;
		}

		x = center.x - hexagonAmbitRadius / 2;
		y = center.y + hexagonInCircleRadius;
		VPoint topLeft = new VPoint(x, y);
		if (!containsPoint(points, topLeft, GeometryUtils.DOUBLE_EPS)
				&& bounds.contains(new VPoint(topLeft.x - hexagonAmbitRadius, topLeft.y + hexagonAmbitRadius))) {
			points.add(topLeft);
			addPoint = true;
		}

		x = center.x + hexagonAmbitRadius / 2;
		y = center.y + hexagonInCircleRadius;
		VPoint topRight = new VPoint(x, y);
		if (!containsPoint(points, topRight, GeometryUtils.DOUBLE_EPS)
				&& bounds.contains(new VPoint(topRight.x + hexagonAmbitRadius, topRight.y + hexagonAmbitRadius))) {
			points.add(topRight);
			addPoint = true;
		}

		x = center.x - hexagonAmbitRadius / 2;
		y = center.y - hexagonInCircleRadius;
		VPoint bottomLeft = new VPoint(x, y);
		if (!containsPoint(points, bottomLeft, GeometryUtils.DOUBLE_EPS)
				&& bounds.contains(new VPoint(bottomLeft.x - hexagonAmbitRadius, bottomLeft.y - hexagonAmbitRadius))) {
			points.add(bottomLeft);
			addPoint = true;
		}


		x = center.x + hexagonAmbitRadius / 2;
		y = center.y - hexagonInCircleRadius;
		VPoint bottomRight = new VPoint(x, y);
		if (!containsPoint(points, bottomRight, GeometryUtils.DOUBLE_EPS) && bounds
				.contains(new VPoint(bottomRight.x + hexagonAmbitRadius, bottomRight.y - hexagonAmbitRadius))) {
			points.add(bottomRight);
			addPoint = true;
		}

		return addPoint;
	}
}
