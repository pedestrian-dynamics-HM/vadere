package org.vadere.util.geometry.shapes;

import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.internal.RectangleDouble;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Geometric shape and position.
 */
public interface VShape extends Shape, Cloneable, Geometry {

	double BORDER_TOLERANCE = 0.1;

	double distance(IPoint point);

	VPoint closestPoint(IPoint point);

	Optional<VPoint> getClosestIntersectionPoint(VPoint q1, VPoint q2, VPoint r);

	boolean contains(IPoint point);

	VShape translate(final IPoint vector);

	VShape translatePrecise(final IPoint vector);

	VShape scale(final double scalar);

	default boolean atBorder(final VPoint point){
		VShape circle = new VCircle(new VPoint(point.getX(), point.getY()), BORDER_TOLERANCE);
		return intersects(circle) && !containsShape(circle);
	}

	default VShape resize(final IPoint start, final IPoint end){
		double startDistance = distanceToCenter(start);
		double endDistance = distanceToCenter(end);
		VPoint center = this.getCentroid();
		VShape scaled = this.scale(endDistance / startDistance);
		return scaled.translatePrecise(center.subtract(scaled.getCentroid()));
	}

	default double distanceToCenter(final IPoint point){
		final int squareExponent = 2;
		double deltaXSquared = Math.pow(point.getX() - this.getCentroid().getX(), squareExponent);
		double deltaYSquared = Math.pow(point.getY() - this.getCentroid().getY(), squareExponent);
		return Math.sqrt(deltaXSquared + deltaYSquared);
	}

	boolean intersects(VLine intersectingLine);

	VPoint getCentroid();

	ShapeType getType();

	/**
	 * {@link VCircle} containing all points of underling shape. similar to bound but a circle
	 * rather than a Rectangle.
	 */
	default VCircle getCircumCircle() {
		Rectangle2D bound = getBounds2D();
		double radius =
				Math.sqrt(bound.getWidth() * bound.getWidth() + bound.getHeight() * bound.getHeight());
		return new VCircle(new VPoint(bound.getCenterX(), bound.getCenterY()), radius);
	}

	default boolean sameArea(VShape shape){
		Area thisShape = new Area(this);
		Area otherShape = new Area(shape);
		thisShape.subtract(otherShape);
		return thisShape.isEmpty();
	}

	// numerical not stable for comparision with VCircle.
	// use contains(VCircle otherShape)
	default boolean containsShape(VShape otherShape) {
		if (otherShape instanceof  VCircle){
			return this.contains((VCircle)otherShape);
		}
		Area thisArea = new Area(this);
		Area otherArea = new Area(otherShape);
		thisArea.intersect(otherArea);
		return thisArea.equals(otherArea);

	}

	// todo: remove default implementation and implement specific performance optimized and numerical stable versions.
	default boolean contains(VCircle otherShape){
		// override in specific shapes for more performance optimized implementations
		Area thisArea = new Area(this);
		Area otherArea = new Area(otherShape);
		thisArea.intersect(otherArea);
		return thisArea.equals(otherArea);
	}

	/**
	 * Returns a list of points (p1, p2, ..., pn) such that the line (p1,p2) is part of the boundary
	 * of the approximation of this shape. p1 != pn i.e. it is not a closed path.
	 *
	 * @return the path which approximates the boundary of this shape
	 */
	List<VPoint> getPath();

	List<VLine> lines();

	static VPolygon generateHexagon(final double radius) {
		List<VPoint> points = new ArrayList<>();

		// in cw-order
		points.add(new VPoint(radius, 0));
		points.add(new VPoint(radius * Math.cos(1.0 / 3.0 * Math.PI), radius * Math.sin(1.0 / 3.0 * Math.PI)));
		points.add(new VPoint(radius * Math.cos(2.0 / 3.0 * Math.PI), radius * Math.sin(2.0 / 3.0 * Math.PI)));
		points.add(new VPoint(-radius, 0));
		points.add(new VPoint(radius * Math.cos(4.0 / 3.0 * Math.PI), radius * Math.sin(4.0 / 3.0 * Math.PI)));
		points.add(new VPoint(radius * Math.cos(5.0 / 3.0 * Math.PI), radius * Math.sin(5.0 / 3.0 * Math.PI)));


		Path2D path2D = new Path2D.Double();

		path2D.moveTo(points.get(0).getX(),points.get(0).getY());
		path2D.lineTo(points.get(0).getX(),points.get(0).getY());

		for(int i = 1; i < points.size(); i++) {
			path2D.lineTo(points.get(i).getX(),points.get(i).getY());
		}

		path2D.lineTo(points.get(0).getX(),points.get(0).getY());

		return new VPolygon(path2D);
	}

	default boolean intersects(VShape shape){
		Area thisShape = new Area(this);
		Area otherShape = new Area(shape);
		Area thisShapeCpy = new Area(this);
		thisShape.subtract(otherShape);
		return !thisShape.equals(thisShapeCpy);
	}

	default int getDirectionalCode(Point startPoint, int directions) {
		return getDirectionalCode(new VPoint(startPoint), directions);
	}

	default int getDirectionalCode(IPoint startPoint, int directions) {
		VPoint direction = new VPoint(startPoint).subtract(getCentroid());
		double angle = Math.atan(direction.getY() / direction.getX());
		angle += Math.PI + Math.PI / (directions);
		double indexRatio = (angle) / (2 * Math.PI);
		return (int)(indexRatio * directions);
	}


	// Methods used by the R-Tree
	@Override
	default double distance(com.github.davidmoten.rtree.geometry.Rectangle rectangle) {
		return mbr().distance(rectangle);
	}

	@Override
	default com.github.davidmoten.rtree.geometry.Rectangle mbr() {
		Rectangle2D bound = getBounds2D();
		return RectangleDouble.create(bound.getMinX(), bound.getMinY(), bound.getMaxX(), bound.getMaxY());
	}

	@Override
	default boolean intersects(com.github.davidmoten.rtree.geometry.Rectangle rectangle) {
		return mbr().intersects(rectangle);
	}

	@Override
	default boolean isDoublePrecision() {
		return true;
	}
}
