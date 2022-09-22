package org.vadere.util.geometry.shapes;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.GrahamScan;

/**
 * Note: A polygon which has the same points as a rectangle is not equals to the rectangle.
 */
public class VPolygon extends Path2D.Double implements VShape {
	private static final long serialVersionUID = 6534837112398242609L;

	public VPolygon(Path2D.Double path) {
		this.reset();
		this.append(path, false);
		this.closePath();
		/*if (!path.getBounds().isEmpty()) {

		}*/
	}

	public VPolygon() {
		this(new Path2D.Double(new VRectangle()));
	}

	public VPolygon(Shape shape) {
		this(new Path2D.Double(shape));
	}

	public boolean isSimple() {
		List<VPoint> points = getPath();
		for(int i = 0; i < points.size(); i++) {
			VPoint p1 = points.get(i);
			VPoint p2 = points.get((i+1) % points.size());
			for(int j = i + 1; j < points.size(); j++) {
				VPoint q1 = points.get(j);
				VPoint q2 = points.get((j+1) % points.size());

				if(GeometryUtils.intersectLineSegment(p1, p2, q1, q2)) {
					return false;
				}
			}
		}

		return true;
	}

	public boolean isCCW() {
		List<VPoint> points = getPath();
		assert points.size() >= 3;
		GrahamScan scan = new GrahamScan(points);
		Set<VPoint> convexHull = new HashSet<>(scan.getConvexHull());
		List<VPoint> convexPartialPolygon = points.stream().filter(p -> convexHull.contains(p)).collect(Collectors.toList());
		return GeometryUtils.isCCW(convexPartialPolygon.get(0), convexPartialPolygon.get(1), convexPartialPolygon.get(2));
	}

	public VPolygon toCCWOrder() {
		if(isCCW()) {
			return this;
		}
		else {
			return revertOrder();
		}
	}

	//TODO: this does only work for certain cases!
	public VPolygon smooth(double angle) {
		List<VPoint> points = getPath();
		assert points.size() >= 3;
		boolean found;
		do {
			found = false;
			for(int i = 0; i <= points.size()+1; i++) {
				VPoint p1 = points.get(i % (points.size()));
				VPoint p2 = points.get((i+1) % (points.size()));
				VPoint p3 = points.get((i+2) % (points.size()));

				if(GeometryUtils.isCCW(p1, p2, p3)) {
					double triAngle = GeometryUtils.angle(p1, p2, p3);
					if(triAngle < angle) {
						points.remove((i+1) % (points.size()));
						found = true;
					}
				}
			}
		} while (found);

		return GeometryUtils.toPolygon(points);
	}

	public VPolygon revertOrder() {
		List<VPoint> points = getPath();
		Collections.reverse(points);
		return GeometryUtils.toPolygon(points);
	}

	public boolean isRectangular(){
		Area tmp = new Area(this);
		return tmp.isRectangular();
	}

	public VRectangle asVRectangle(){
		if (!isRectangular())
			return null;

		// retuns list of points.
		// width points[0] -- points[1]
		// height points[0] -- points[3]
		ArrayList<VPoint> points = new ArrayList<>(getPoints());
		assert points.size() == 4 : "This is not a VRectangle";
		double width = points.get(0).distance(points.get(1));
		double height = points.get(0).distance(points.get(3));
		return new VRectangle(points.get(0).x, points.get(0).y, width, height);
	}

	/**
	 * Check whether the given polygon intersects with the open ball around
	 * "center" with given radius.
	 * 
	 * @param center the center of the open ball
	 * @param radius the radius of the open ball
	 * @return true if any point of the polygon lies within the open ball.
	 */
	public boolean intersects(VPoint center, double radius) {
		// if the center is contained in the polygon, parts of the ball are
		// contained as well
		if (this.contains(center)) {
			return true;
		}

		// check whether the center is closer to the sides than the radius
		// loop over all lines and check intersection
		List<VPoint> pointList = getPoints();
		for (int i = 0; i < pointList.size() - 1; i++) {
			VLine intersectingLine;
			// loop around
			if (i < pointList.size() - 1) {
				intersectingLine = new VLine(pointList.get(i),
						pointList.get(i + 1));
			} else {
				intersectingLine = new VLine(pointList.get(i), pointList.get(0));
			}

			// check distance of closest point on the line to the center of the
			// ball
			if (GeometryUtils.closestToSegment(intersectingLine, center)
					.distance(center) < radius) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns a list of all points of this geometry.
	 *
	 * @return A list of points.
	 */
	public List<VPoint> getPoints() {
		List<VPoint> resultList = new ArrayList<>(); // use ArrayList for better index retrieval

		PathIterator iterator = this.getPathIterator(null);
		double[] coords = new double[6];
		while (!iterator.isDone()) {
			int type = iterator.currentSegment(coords);
			iterator.next();
			if (type == PathIterator.SEG_LINETO) {
				resultList.add(new VPoint(coords[0], coords[1]));
			}
		}

		return resultList;
	}

	public boolean intersects(VLine intersectingLine) {

		// check whether the center is closer to the sides than the radius
		// loop over all lines and check intersection
		List<VPoint> pointList = getPoints();
		for (int i = 0; i < pointList.size(); i++) {
			VLine polyLine;
			// loop around
			if (i < pointList.size() - 1) {
				polyLine = new VLine(pointList.get(i), pointList.get(i + 1));
			} else {
				polyLine = new VLine(pointList.get(i), pointList.get(0));
			}

			// check distance of closest point on the line to the center of the
			// ball
			if (polyLine.intersectsLine(intersectingLine)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether two polygons (this and another polygon) do intersect.
	 *
	 * @param intersectingPolygon   the other polygon
	 * @return true if the two polygons intersect
	 */
	public boolean intersects(final VPolygon intersectingPolygon) {

		if(containsShape(intersectingPolygon) || intersectingPolygon.containsShape(this)) {
			return true;
		}

		List<VPoint> pointList = getPoints();
		for (int i = 0; i < pointList.size() - 1; i++) {
			VLine polyLine;
			// loop around
			if (i < pointList.size() - 1) {
				polyLine = new VLine(pointList.get(i), pointList.get(i + 1));
			} else {
				polyLine = new VLine(pointList.get(i), pointList.get(0));
			}

			// check if current line intersects with given polygon
			if (intersectingPolygon.intersects(polyLine)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks whether this polygon and the rectangle intersects.
	 *
	 * @param rectangle   the rectangle
	 * @return true if this polygon intersects with the rectangle
	 */
	public boolean intersects(final VRectangle rectangle) {

		if(containsShape(rectangle) || rectangle.containsShape(this)) {
			return true;
		}

		return intersectsRectangleLine(rectangle);
	}

	public boolean intersectsRectangleLine(final VRectangle rectangle) {
		return intersects(new VLine(rectangle.getMinX(), rectangle.getMinY(), rectangle.getMaxX(), rectangle.getMinY()))
			|| intersects(new VLine(rectangle.getMaxX(), rectangle.getMinY(), rectangle.getMaxX(), rectangle.getMaxY()))
			|| intersects(new VLine(rectangle.getMaxX(), rectangle.getMaxY(), rectangle.getMinX(), rectangle.getMaxY()))
			|| intersects(new VLine(rectangle.getMinX(), rectangle.getMaxY(), rectangle.getMinX(), rectangle.getMinY()));
	}

	public double getArea() {
		return GeometryUtils.areaOfPolygon(getPoints());
	}

	// Assumed that first and last point are equal
	public void grow(double absolute) {
		LinkedList<VPoint> curVertices = new LinkedList<VPoint>();
		LinkedList<VPoint> newVertices = new LinkedList<VPoint>();
		VPoint lastVertex = VPoint.ZERO;
		VPoint curVertex = VPoint.ZERO;
		VPoint nxtVertex = VPoint.ZERO;
		VPoint deltaCurLast = VPoint.ZERO;
		VPoint deltaNxtCur = VPoint.ZERO;
		VPoint deltaNxtLast = VPoint.ZERO;
		double coord[] = new double[2];
		double distCurLast;
		double distNxtCur;
		double distNxtLastScaled;

		for (PathIterator vertexItr = getPathIterator(null); !vertexItr
				.isDone(); vertexItr.next()) {
			vertexItr.currentSegment(coord);
			curVertices.add(new VPoint(coord[0], coord[1]));
		}

		/*
		 * One or two vertices do not define a plane and hence growing within
		 * the given meaning is impossible.
		 */
		if (curVertices.size() < 3) {
			return;
		}

		lastVertex = curVertices.get(curVertices.size() - 2);
		curVertex = curVertices.getFirst();

		for (int iVertex = 0; iVertex < curVertices.size() - 1; ++iVertex) {
			nxtVertex = curVertices.get(iVertex + 1);

			distCurLast = curVertex.distance(lastVertex);
			double x = (curVertex.x - lastVertex.x) / distCurLast;
			double y = (curVertex.y - lastVertex.y) / distCurLast;

			deltaCurLast = new VPoint(x, y);

			distNxtCur = curVertex.distance(nxtVertex);
			x = (nxtVertex.x - curVertex.x) / distNxtCur;
			y = (nxtVertex.y - curVertex.y) / distNxtCur;
			deltaNxtCur = new VPoint(x, y);

			x = (deltaNxtCur.x + deltaCurLast.x);
			y = (deltaNxtCur.y + deltaCurLast.y);
			deltaNxtLast = new VPoint(x, y);
			distNxtLastScaled = deltaNxtLast.distance(new VPoint(0, 0));

			x = deltaNxtLast.x / distNxtLastScaled * absolute;
			y = deltaNxtLast.y / distNxtLastScaled * absolute;
			deltaNxtLast = new VPoint(x, y);

			newVertices.add(new VPoint(curVertex.x + deltaNxtLast.y,
					curVertex.y - deltaNxtLast.x));

			lastVertex = curVertex;
			curVertex = nxtVertex;

		}

		newVertices.add(newVertices.getFirst());

		this.reset();
		if (!newVertices.isEmpty()) {
			this.moveTo(newVertices.get(0).x, newVertices.get(0).y);
			this.append(GeometryUtils.polygonFromPoints2D(newVertices
					.toArray(new VPoint[0])), false);
			this.closePath();
		}
	}

	public LinkedList<VPolygon> borderAsShapes(double borderWidth, double shapeShrinkOffset, double segmentGrowOffset) {
		LinkedList<VPolygon> border = new LinkedList<VPolygon>();
		PathIterator vertexItr = getPathIterator(null);
		double lastVertex[] = null;
		double curVertex[] = new double[2];
		double delta[] = new double[2];
		double dist;
		double borderOffset = borderWidth / 2.0;

		vertexItr.currentSegment(curVertex);
		vertexItr.next();

		while (!vertexItr.isDone()) {
			Path2D.Double segmentVertices = new Path2D.Double();

			lastVertex = curVertex.clone();
			int type = vertexItr.currentSegment(curVertex);
			if (type == java.awt.geom.PathIterator.SEG_CLOSE) {
				break;
			}

			delta[0] = curVertex[0] - lastVertex[0];
			delta[1] = curVertex[1] - lastVertex[1];
			dist = Math.sqrt(delta[0] * delta[0] + delta[1] * delta[1]);
			// normalize and scale
			delta[0] = delta[0] / dist;
			delta[1] = delta[1] / dist;

			segmentVertices
					.moveTo(lastVertex[0]
									- delta[0]
									* segmentGrowOffset
									- delta[1]
									* (borderOffset + shapeShrinkOffset + segmentGrowOffset),
							lastVertex[1]
									- delta[1]
									* segmentGrowOffset
									+ delta[0]
									* (borderOffset + shapeShrinkOffset + segmentGrowOffset));
			segmentVertices
					.lineTo(lastVertex[0]
									- delta[0]
									* segmentGrowOffset
									+ delta[1]
									* (borderOffset - shapeShrinkOffset + segmentGrowOffset),
							lastVertex[1]
									- delta[1]
									* segmentGrowOffset
									- delta[0]
									* (borderOffset - shapeShrinkOffset + segmentGrowOffset));
			segmentVertices
					.lineTo(curVertex[0]
									+ delta[0]
									* segmentGrowOffset
									+ delta[1]
									* (borderOffset - shapeShrinkOffset + segmentGrowOffset),
							curVertex[1]
									+ delta[1]
									* segmentGrowOffset
									- delta[0]
									* (borderOffset - shapeShrinkOffset + segmentGrowOffset));
			segmentVertices
					.lineTo(curVertex[0]
									+ delta[0]
									* segmentGrowOffset
									- delta[1]
									* (borderOffset + shapeShrinkOffset + segmentGrowOffset),
							curVertex[1]
									+ delta[1]
									* segmentGrowOffset
									+ delta[0]
									* (borderOffset + shapeShrinkOffset + segmentGrowOffset));

			/* Insert first vertex as last too. */
			segmentVertices
					.lineTo(lastVertex[0]
									- delta[0]
									* segmentGrowOffset
									- delta[1]
									* (borderOffset + shapeShrinkOffset + segmentGrowOffset),
							lastVertex[1]
									- delta[1]
									* segmentGrowOffset
									+ delta[0]
									* (borderOffset + shapeShrinkOffset + segmentGrowOffset));

			border.add(new VPolygon(segmentVertices));

			vertexItr.next();
		}

		return border;
	}

	@Override
	public double distance(IPoint target) {
		try {
			if (contains(target)) {
				return -closestPoint(target).distance(target);
			} else {
				return closestPoint(target).distance(target);
			}
		} catch (NullPointerException ex) {
			if (contains(target)) {
				return -closestPoint(target).distance(target);
			} else {
				return closestPoint(target).distance(target);
			}
		}

	}

	@Override
	public VPoint closestPoint(IPoint point) {
		double currentMinDistance = java.lang.Double.MAX_VALUE;
		VPoint resultPoint = null;

		PathIterator iterator = this.getPathIterator(null);

		double[] first = null;
		double[] last = new double[2];
		double[] next = new double[2];
		VPoint currentClosest;

		iterator.currentSegment(next);
		iterator.next();

		while (!iterator.isDone()) {
			last[0] = next[0];
			last[1] = next[1];

			if(first == null) {
				first = new double[]{last[0], last[1]};
			}

			iterator.currentSegment(next);

			currentClosest = GeometryUtils.closestToSegment(new VLine(last[0],
					last[1], next[0], next[1]), point);

			if (currentClosest.distance(point) < currentMinDistance) {
				currentMinDistance = currentClosest.distance(point);
				resultPoint = currentClosest;
			}

			iterator.next();
		}

		// dont forget the last and first point!
		if(first != null) {
			currentClosest = GeometryUtils.closestToSegment(new VLine(next[0],
					next[1], first[0], first[1]), point);

			if (currentClosest.distance(point) < currentMinDistance) {
				currentMinDistance = currentClosest.distance(point);
				resultPoint = currentClosest;
			}
		}

		return resultPoint;
	}

	@Override
	public Optional<VPoint> getClosestIntersectionPoint(VPoint q1, VPoint q2, VPoint r) {
		double currentMinDistance = java.lang.Double.MAX_VALUE;
		VPoint resultPoint = null;

		PathIterator iterator = this.getPathIterator(null);

		double[] first = null;
		double[] last = new double[2];
		double[] next = new double[2];
		VPoint currentIntersectionPoint;

		iterator.currentSegment(next);
		iterator.next();

		while (!iterator.isDone()) {
			last[0] = next[0];
			last[1] = next[1];

			if(first == null) {
				first = new double[]{last[0], last[1]};
			}

			iterator.currentSegment(next);
			VLine line = new VLine(last[0], last[1], next[0], next[1]);
			if(GeometryUtils.intersectLine(line, q1, q2)) {
				currentIntersectionPoint = GeometryUtils.lineIntersectionPoint(new VLine(last[0],
						last[1], next[0], next[1]), q1.getX(), q1.getY(), q2.getX(), q2.getY());

				if (currentIntersectionPoint.distance(r) < currentMinDistance) {
					currentMinDistance = currentIntersectionPoint.distance(r);
					resultPoint = currentIntersectionPoint;
				}
			}
			iterator.next();
		}

		// dont forget the last and first point!
		if(first != null) {
			VLine line = new VLine(last[0], last[1], next[0], next[1]);
			if(GeometryUtils.intersectLine(line, q1, q2)) {
				currentIntersectionPoint = GeometryUtils.lineIntersectionPoint(new VLine(last[0],
						last[1], next[0], next[1]), q1.getX(), q1.getY(), q2.getX(), q2.getY());

				if (currentIntersectionPoint.distance(r) < currentMinDistance) {
					currentMinDistance = currentIntersectionPoint.distance(r);
					resultPoint = currentIntersectionPoint;
				}
			}
		}

		return Optional.ofNullable(resultPoint);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof VPolygon))
			return false;

		VPolygon other = (VPolygon) obj;

		List<VPoint> thisPoints = this.getPoints();
		List<VPoint> otherPoints = other.getPoints();

		if (!thisPoints.equals(otherPoints))
			return false;

		return true;
	}

	@Override
	public boolean contains(final IPoint point) {
		return super.contains(point.getX(), point.getY());
	}

	@Override
	public VPolygon translatePrecise(final IPoint vector) {
		return translate(vector);
	}

	@Override
	public VPolygon translate(final IPoint vector) {
		AffineTransform transform = new AffineTransform();
		transform.translate(vector.getX(), vector.getY());
		return new VPolygon(new Path2D.Double(this, transform));
	}

	@Override
	public VPolygon scale(final double scalar) {
		AffineTransform transform = new AffineTransform();
		transform.scale(scalar, scalar);
		return new VPolygon(new Path2D.Double(this, transform));
	}

	/**
	 * based on https://stackoverflow.com/a/2792459
	 */
	@Override
	public VPoint getCentroid() {
//		List<VPoint> pointList = getPoints();
//		double area = 0;
//		double xValue = 0;
//		double yValue = 0;
//		for (int i = 0; i < pointList.size() - 1; i++) {
//			area += pointList.get(i).getX() * pointList.get(i + 1).getY()
//					- pointList.get(i).getY() * pointList.get(i + 1).getX();
//			xValue += (pointList.get(i).getX() + pointList.get(i + 1).getX())
//					* (pointList.get(i).getX() * pointList.get(i + 1).getY()
//					- pointList.get(i).getY() * pointList.get(i + 1).getX());
//			yValue += (pointList.get(i).getY() + pointList.get(i + 1).getY())
//					* (pointList.get(i).getX() * pointList.get(i + 1).getY()
//					- pointList.get(i).getY() * pointList.get(i + 1).getX());
//		}
//
//		// last with first point. This is outside of the loop to remove modulo operation
//		// only needed in the last loop.
//		int i = pointList.size() - 1;
//		area += pointList.get(i).getX() * pointList.get(0).getY()
//				- pointList.get(i).getY() * pointList.get(0).getX();
//		xValue += (pointList.get(i).getX() + pointList.get(0).getX())
//				* (pointList.get(i).getX() * pointList.get(0).getY()
//				- pointList.get(i).getY() * pointList.get(0).getX());
//		yValue += (pointList.get(i).getY() + pointList.get(0).getY())
//				* (pointList.get(i).getX() * pointList.get(0).getY()
//				- pointList.get(i).getY() * pointList.get(0).getX());
//
//		area /= 2;
//		xValue /= (6 * area);
//		yValue /= (6 * area);
//
//		return new VPoint(xValue, yValue);
		return GeometryUtils.getPolygonCentroid(getPoints());
	}

	public VPolygon rotate(IPoint anchor, double angle) {
		VPolygon resultPolygon = new VPolygon(this);
		resultPolygon.transform(AffineTransform.getRotateInstance(angle, anchor.getX(), anchor.getY()));
		return resultPolygon;
	}

	@Override
	public ShapeType getType() {
		return ShapeType.POLYGON;
	}

	@Override
	public boolean intersects(final VShape shape) {
		if(shape instanceof VPolygon) {
			return intersects((VPolygon) shape);
		}
		else if(shape instanceof VRectangle){
			return intersectsRectangleLine(((VRectangle)shape));
		}
		else {
			return VShape.super.intersects(shape);
		}
	}

	public List<VLine> getLinePath() {
		List<VPoint> points = getPoints();
		List<VLine> lines = new ArrayList<>(points.size());
		for(int i = 0; i < points.size(); i++) {
			VLine line = new VLine(points.get(i), points.get((i+1) % points.size()));
			lines.add(line);
		}
		return lines;
	}

	@Override
	public List<VPoint> getPath() {
		return getPoints();
	}

	@Override
	public List<VLine> lines() {
		return getLinePath();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		for(VPoint point : getPoints()) {
			builder.append("[" + point.getX() + "," + point.getY() + "],");
		}
		// remove the last ","
		builder.deleteCharAt(builder.length()-1);
		builder.append("]");
		return builder.toString();
	}

}
