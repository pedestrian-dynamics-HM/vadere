package org.vadere.util.geometry;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

/**
 * A generic geometry. Represented by a polygon (borders) with inner polygons
 * (obstacles), if any.
 * 
 * 
 */
public class Geometry {

	private Set<VPolygon> polygons;
	private Rectangle2D boundary;

	public Geometry(VPolygon... polygons) {
		this.polygons = new HashSet<>(Arrays.asList(polygons));
		this.boundary = new Rectangle2D.Double();
		for (VPolygon polygon : polygons) {
			this.boundary = polygon.getBounds2D().createUnion(
					this.boundary);
		}
	}

	/**
	 * Returns a list of all points of this geometry.
	 * 
	 * @return A list of points.
	 */
	public List<VPoint> getPoints() {
		List<VPoint> resultList = new LinkedList<VPoint>();

		// add obstacles
		for (VPolygon p : this.polygons) {
			resultList.addAll(p.getPoints());
		}

		return resultList;
	}

	/**
	 * Returns the list of all polygons of this geometry, including the
	 * boundary.
	 * 
	 * @return
	 */
	public List<VPolygon> getPolygons() {
		return new LinkedList<VPolygon>(this.polygons);
	}

	/**
	 * Checks if a given point is in the geometry, including inner polygons.
	 * 
	 * @param toCheck
	 *        Point to check
	 */
	public boolean contains(VPoint toCheck) {
		for (VPolygon p : polygons) {
			if (p.contains(toCheck)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines whether a point lies on the boundary of this geometry.
	 * 
	 * @param p
	 * @return
	 */
	public boolean onBoundary(Point p) {
		throw new UnsupportedOperationException(
				"onBoundary is not implemented yet.");
	}

	/**
	 * Checks intersection with a given polygon
	 * 
	 * @param polygon
	 * @param createMidpoints
	 *        if true, every line is splitEdge into two by inserting a midpoint.
	 * @return
	 */
	public boolean intersects(VPolygon polygon, boolean createMidpoints,
			boolean useEndpoints) {

		List<VPoint> pointList = getPoints();
		List<VPoint> points = new LinkedList<VPoint>(pointList);

		// if midpoints should be created, loop over the pointList and create
		// them
		if (createMidpoints && !pointList.isEmpty()) {
			for (int i = 0; i < pointList.size() - 1; i++) {
				points.add(GeometryUtils.interpolate(pointList.get(i),
						pointList.get(i + 1), 0.5));
			}
			VPoint interpolated = GeometryUtils.interpolate(
					pointList.get(pointList.size() - 1), pointList.get(0), 0.5);
			points.add(interpolated);
		}

		// add first point so we get a closed loop
		if (!pointList.isEmpty()) {
			points.add(points.get(0));
		}


		// create the geometry to check intersection with
		// TODO [priority=low] [task=refactoring] maybe check with polygon only?
		// Geometry geometry = new Geometry();
		// geometry.addPolygon(polygon);


		// loop over all lines and check intersection
		for (int i = 0; i < points.size() - 1; i++) {
			VLine intersectingLine = new VLine(points.get(i), points.get(i + 1));

			if (polygon.intersects(intersectingLine)) {
				return true;
			}
		}

		// loop over this points
		for (VPoint p : pointList) {
			if (polygon.contains(p)) {
				return true;
			}
		}

		// loop over polygons points
		for (VPoint p : polygon.getPoints()) {
			if (this.contains(p)) {
				return true;
			}
		}

		return false;
	}

	public Rectangle2D getBounds2D() {
		return this.boundary;
	}

	public void addPolygon(VPolygon polygon) {
		this.polygons.add(polygon);
		this.boundary = this.boundary.createUnion(polygon.getBounds2D());
	}

	public boolean intersect(VLine line) {
		for (VPolygon polygon : polygons) {
			if (polygon.intersects(line)) {
				return true;
			}
		}
		return false;
	}
}
