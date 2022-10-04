package org.vadere.util.geometry.shapes;

import com.github.davidmoten.rtree.geometry.Rectangle;
import com.github.davidmoten.rtree.geometry.internal.RectangleDouble;

import org.vadere.util.geometry.GeometryUtils;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("serial")
/**
 * Note: A rectangle which has the same corner points as a polygon is not
 * equals to the polygon.
 */
public class VRectangle extends Rectangle2D.Double implements VShape {

	/**
	 * The x and y define the corner of the rectangle with the smallest values.
	 *
	 * @param x x-coordinate of the lower-left corner
	 * @param y y-coordinate of the lower-left corner
	 * @param w the width of the rectangle
	 * @param h the height of the rectangle
	 */
	public VRectangle(double x, double y, double w, double h) {
		super(x, y, w, h);

		if (w <= 0 || h <= 0) {
			throw new IllegalArgumentException(
					"Width and height have to be positive.");
		}
	}

	public VRectangle(){
		super(0,0,1,1);
	}

	public VRectangle(final Rectangle2D rectangle) {
		this(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
	}

	public VRectangle(final Rectangle2D.Double rectangle) {
		this(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
	}

	@Override
	public boolean contains(VCircle otherShape) {
		double centerX = otherShape.getCentroid().x;
		double centerY = otherShape.getCentroid().y;
		double radius = otherShape.getRadius();

		boolean circleFitsIntoHorizontally = (centerX > (this.x + radius)) && ((this.x + this.width) > (centerX + radius));
		boolean circleFitsIntoVertically = (centerY > (this.y + radius)) && ((this.y + this.height) > (centerY + radius));

		return circleFitsIntoHorizontally && circleFitsIntoVertically;
	}

	@Override
	public double distance(IPoint point) {
		VPoint closestPoint = closestPoint(point);

		if (contains(point)) {
			return -closestPoint.distance(point);
		} else {
			return closestPoint.distance(point);
		}
	}

	@Override
	public VPoint closestPoint(IPoint point) {
		VLine[] lines = getLines();
		VPoint result = GeometryUtils.closestToSegment(lines[0], point);
		double distanceToLine = result.distance(point);

		for (int i = 1; i < 4; i++) {
			VPoint tmpClosest = GeometryUtils.closestToSegment(lines[i], point);
			double tmpDistance = tmpClosest.distance(point);
			if (tmpDistance < distanceToLine) {
				distanceToLine = tmpDistance;
				result = tmpClosest;
			}
		}

		return result;
	}

	@Override
	public Optional<VPoint> getClosestIntersectionPoint(VPoint q1, VPoint q2, VPoint r) {
		double minDinstance = java.lang.Double.MAX_VALUE;
		VPoint intersectionPoint = null;
		for(VLine line : getLines()) {
			if(GeometryUtils.intersectLineSegment(line, q1, q2)) {
				VPoint tmpIntersectionPoint = GeometryUtils.lineIntersectionPoint(line, q1.getX(), q1.getY(), q2.getX(), q2.getY());
				double distance = tmpIntersectionPoint.distance(r);
				if(distance < minDinstance) {
					minDinstance = distance;
					intersectionPoint = tmpIntersectionPoint;
				}
			}
		}
		return Optional.ofNullable(intersectionPoint);
	}

	public VLine[] getLines() {
		VLine[] result = new VLine[4];

		result[0] = new VLine(x, y, x + width, y);
		result[1] = new VLine(x + width, y, x + width, y + height);
		result[2] = new VLine(x + width, y + height, x, y + height);
		result[3] = new VLine(x, y + height, x, y);

		return result;
	}

	public VPoint[] getCornerPoints() {
		VPoint[] result = new VPoint[4];

		result[0] = new VPoint(x, y);
		result[1] = new VPoint(x + width, y);
		result[2] = new VPoint(x + width, y + height);
		result[3] = new VPoint(x, y + height);

		return result;
	}

	public double getArea() {
		return getWidth() * getHeight();
	}

	@Override
	public boolean contains(IPoint point) {
		return super.contains(point.getX(), point.getY());
	}

	@Override
	public VRectangle translatePrecise(final IPoint vector) {
		VPoint dp = VPoint.addPrecise(vector, new VPoint(getX(), getY()));
		return new VRectangle(dp.getX(), dp.getY(), getWidth(), getHeight());
	}

	@Override
	public VRectangle translate(final IPoint vector) {
		return new VRectangle(getX() + vector.getX(), getY() + vector.getY(), getWidth(), getHeight());
	}

	@Override
	public VRectangle scale(final double scalar) {
		return new VRectangle(getX() * scalar, getY() * scalar, getWidth() * scalar, getHeight() * scalar);
	}

	@Override
	public VRectangle resize(IPoint start, IPoint end){
		double minX = Math.abs(start.getX() - getMinX()) < BORDER_TOLERANCE ? end.getX() : getMinX();
		double minY = Math.abs(start.getY() - getMinY())  < BORDER_TOLERANCE ? end.getY() : getMinY();

		double maxX    = Math.abs(start.getX() - getMaxX()) < BORDER_TOLERANCE ? end.getX() : getMaxX();
		double maxY    = Math.abs(start.getY() - getMaxY()) < BORDER_TOLERANCE ? end.getY() : getMaxY();

		return new VRectangle(minX, minY, maxX - minX, maxY - minY);
	}

	@Override
	public int getDirectionalCode(IPoint startPoint, int directions){
		double horizontalRatio = (startPoint.getX() - getCenterX()) / (getWidth() / 2);
		double verticalRatio = (startPoint.getY() - getCenterY()) / (getHeight() / 2);
		if (Math.abs(horizontalRatio - verticalRatio) < BORDER_TOLERANCE) {
			return horizontalRatio > 0 ? 1 : 5;
		} else if (Math.abs(horizontalRatio + verticalRatio) < BORDER_TOLERANCE) {
			return horizontalRatio > 0 ? 3 : 7;
		} else if (Math.abs(horizontalRatio) > Math.abs(verticalRatio)) {
			return horizontalRatio > 0 ? 0 : 4;
		}
		return verticalRatio > 0 ? 2 : 6;
	}

	@Override
	public boolean intersects(VLine intersectingLine) {
		if (intersectingLine.intersects(this)) {
			return true;
		}
		return false;
	}

	@Override
	public VPoint getCentroid() {
		return new VPoint(x + (width / 2), y + (height / 2));
	}

	@Override
	public ShapeType getType() {
		return ShapeType.RECTANGLE;
	}

	public VPolygon toPolygon() {
		return new VPolygon(this);
	}

	@Override
	public boolean intersects(final VShape shape) {
		if(shape instanceof VRectangle){
			return super.intersects(((VRectangle)shape));
		}
		else if(shape instanceof VPolygon) {
			return ((VPolygon)shape).intersects(this);
		}
		else {
			return VShape.super.intersects(shape);
		}
	}

	@Override
	public List<VPoint> getPath() {
		return Arrays.asList(new VPoint(x,y), new VPoint(x+width, y), new VPoint(x+width, y+height), new VPoint(x, y+height));
	}

	@Override
	public List<VLine> lines() {
		List<VLine> lines = new ArrayList<>();
		for (VLine line : getLines()) {
			lines.add(line);
		}
		return lines;
	}
}
