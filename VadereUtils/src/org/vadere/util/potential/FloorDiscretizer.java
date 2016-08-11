package org.vadere.util.potential;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.LinkedList;

import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

/**
 * Provides utility methods to set the values of sampling points of a grid,
 * covered by the footprint of one or several bodies. Generally these methods
 * are used to write ScenarioElements of a floor to a floor field. This may be
 * regarded as floor discretization.
 */
public class FloorDiscretizer {

	/**
	 * Sets the value of the grid points covered by the footprints of the given
	 * bodies to 'value'.
	 */
	public static void setGridValueForShapes(CellGrid floorGrid,
			Collection<VShape> elementShapes, CellState value) {
		for (VShape b : elementShapes) {
			setGridValuesForShape(floorGrid, b, value);
		}
	}

	/**
	 * Sets the value of the grid points covered by the footprint of the given
	 * scenario element to 'value'.
	 */
	public static void setGridValuesForShape(CellGrid floorGrid,
			VShape elementShape, CellState value) {
		// Axis aligned bounds of the given body.
		Rectangle2D bodyBounds = elementShape.getBounds2D();

		// Nearest points on the grid of the left lower and right upper corners
		// of the bounds of the body. These represent the outer bounds of the
		// grid points to write.
		Point pointLeftLower, pointRightUpper;

		// Compute nearest grid point of the lower left corner of bodyBounds.
		pointLeftLower = floorGrid.getNearestPoint(bodyBounds.getMinX(),
				bodyBounds.getMinY());
		// Compute nearest grid point of the right upper corner of bodyBounds.
		pointRightUpper = floorGrid.getNearestPoint(bodyBounds.getMaxX(),
				bodyBounds.getMaxY());

		// Run through all grid points of bounds and set its value if being
		// covered by the bodies footprint.
		for (int x = pointLeftLower.x - 1; x <= pointRightUpper.x + 1; ++x) {
			for (int y = pointLeftLower.y - 1; y <= pointRightUpper.y + 1; ++y) {
				// Convert the grid point to the bodies coordinate system and
				// verify if lies within the bodies shape.
				if (elementShape.contains(floorGrid.pointToCoord(x, y)) && floorGrid.isValidPoint(new Point(x, y))) {
					floorGrid.setValue(x, y, value.clone());
				}
			}
		}
	}

	/**
	 * Similar to
	 * {@link #setGridDistanceValuesForShape(CellGrid, VShape, PathFindingTag, PathFindingTag)},
	 * but sets the values considering the CENTER of the cells, not the left lower corner.
	 */
	public static void setGridValuesForShapeCentered(CellGrid floorGrid,
			VShape elementShape, CellState value) {
		// Axis aligned bounds of the given body.
		Rectangle2D bodyBounds = elementShape.getBounds2D();

		// Nearest points on the grid of the left lower and right upper corners
		// of the bounds of the body. These represent the outer bounds of the
		// grid points to write.
		Point pointLeftLower, pointRightUpper;

		// Compute nearest grid point of the lower left corner of bodyBounds.
		pointLeftLower = floorGrid.getNearestPoint(bodyBounds.getMinX(),
				bodyBounds.getMinY());
		// Compute nearest grid point of the right upper corner of bodyBounds.
		pointRightUpper = floorGrid.getNearestPoint(bodyBounds.getMaxX(),
				bodyBounds.getMaxY());

		double dx = floorGrid.width / (floorGrid.getNumPointsX() - 1);
		double dy = floorGrid.height / (floorGrid.getNumPointsY() - 1);

		// Run through all grid points of bounds and set its value if being
		// covered by the bodies footprint.
		for (int x = pointLeftLower.x - 1; x <= pointRightUpper.x + 1; ++x) {
			for (int y = pointLeftLower.y - 1; y <= pointRightUpper.y + 1; ++y) {
				// Convert the grid point to the bodies coordinate system and
				// verify if lies within the bodies shape. For the element shape
				// check, use the center point on the grid, not the left lower corner.
				VPoint p = floorGrid.pointToCoord(x, y).add(new Vector2D(dx / 2, dy / 2));
				if (elementShape.contains(p) && floorGrid.isValidPoint(new Point(x, y))) {
					floorGrid.setValue(x, y, value.clone());
				}
			}
		}
	}

	/**
	 * Computes the distance to the given shape of all contained plus directly adjacent points.
	 * A point is considered adjacent if one of its eight neighbors is contained in the shape.
	 * 
	 * @deprecated not used by any function but its test. The reason for this function was that the
	 *             target shapes were not correctly rendered on the grid. They are now, in the
	 *             FastMarching algorithm itself.
	 */
	public static void setGridDistanceValuesForShape(CellGrid floorGrid, VShape elementShape, PathFindingTag tagInside,
			PathFindingTag tagOutside) {
		// Axis aligned bounds of the given body.
		Rectangle2D bodyBounds = elementShape.getBounds2D();

		// Nearest points on the grid of the left lower and right upper corners
		// of the bounds of the body. These represent the outer bounds of the
		// grid points to write.
		Point pointLeftLower, pointRightUpper;

		double eps = GeometryUtils.DOUBLE_EPS;

		// Compute nearest grid point of the lower left corner of bodyBounds.
		pointLeftLower = floorGrid.getNearestPoint(bodyBounds.getMinX() - eps,
				bodyBounds.getMinY() - eps);
		// Compute nearest grid point of the right upper corner of bodyBounds.
		pointRightUpper = floorGrid.getNearestPoint(bodyBounds.getMaxX() + eps,
				bodyBounds.getMaxY() + eps);

		// Run through all grid points of bounds and set its value if being
		// covered by the bodies footprint.
		for (int x = pointLeftLower.x - 1; x <= pointRightUpper.x + 1; ++x) {
			for (int y = pointLeftLower.y - 1; y <= pointRightUpper.y + 1; ++y) {
				// Convert the grid point to the bodies coordinate system and
				// verify if lies within the bodies shape.
				boolean isCloseToShape = false;

				for (int xx = x - 1; xx <= x + 1; xx++) {
					for (int yy = y - 1; yy <= y + 1; yy++) {
						if (elementShape.contains(floorGrid.pointToCoord(xx, yy))) {
							isCloseToShape = true;
						}
					}
				}

				if (isCloseToShape && floorGrid.isValidPoint(new Point(x, y))) {
					double dist = elementShape.distance(new VPoint(x, y));
					if (dist < eps) {
						floorGrid.setValue(x, y, new CellState(0.0, tagInside));
					} else {
						floorGrid.setValue(x, y, new CellState(dist, tagOutside));
					}
				}
			}
		}
	}

	/**
	 * Returns the sampling points of the given grid which are covered by the
	 * footprint of the body.
	 */
	public static LinkedList<Point> getShapeFootprint(CellGrid floorGrid,
			VShape elementShape) {
		LinkedList<Point> footprint = new LinkedList<Point>();

		/* Axis aligned bounds of the given body. */
		Rectangle2D obstacleBounds = elementShape.getBounds();

		/*
		 * Nearest points on the grid of the left lower and right upper corners
		 * of the bounds of the body. These represent the outer bounds of the
		 * grid points to write.
		 */
		Point pointLeftLower, pointRightUpper;

		/* Compute nearest grid point of the lower left corner of bodyBounds. */
		pointLeftLower = floorGrid.getNearestPoint(obstacleBounds.getMinX(),
				obstacleBounds.getMinY());
		/* Compute nearest grid point of the right upper corner of bodyBounds. */
		pointRightUpper = floorGrid.getNearestPoint(obstacleBounds.getMaxX(),
				obstacleBounds.getMaxY());

		/*
		 * Run through all grid points of bounds and set its value if being
		 * covered by the bodies footprint.
		 */
		for (int x = pointLeftLower.x; x <= pointRightUpper.x; ++x) {
			for (int y = pointLeftLower.y; y <= pointRightUpper.y; ++y) {
				/*
				 * Convert the grid point to the bodies coordinate system. Add
				 * to 'footprint' if it lies within the bodies shape.
				 */
				if (elementShape.contains(floorGrid.pointToCoord(x, y))) {
					footprint.add(new Point(x, y));
				}
			}
		}

		return footprint;
	}
}
