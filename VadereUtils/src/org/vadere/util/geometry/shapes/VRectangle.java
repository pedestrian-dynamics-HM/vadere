package org.vadere.util.geometry.shapes;

import java.awt.geom.Rectangle2D;

import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.ShapeType;

@SuppressWarnings("serial")
public class VRectangle extends Rectangle2D.Double implements VShape {

	/**
	 * The x and y define the corner of the rectangle with the smallest values.
	 */
	public VRectangle(double x, double y, double w, double h) {
		super(x, y, w, h);

		if (w <= 0 || h <= 0) {
			throw new IllegalArgumentException(
					"Width and height have to be positive.");
		}
	}

	public VRectangle(final Rectangle2D.Double rectangle) {
		this(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
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
	public VShape translatePrecise(final IPoint vector) {
		VPoint dp = VPoint.addPrecise(vector, new VPoint(getX(), getY()));
		return new VRectangle(dp.getX(), dp.getY(), getWidth(), getHeight());
	}

	@Override
	public VShape translate(final IPoint vector) {
		return new VRectangle(getX() + vector.getX(), getY() + vector.getY(), getWidth(), getHeight());
	}

	@Override
	public VShape scale(final double scalar) {
		return new VRectangle(getX() * scalar, getY() * scalar, getWidth() * scalar, getHeight() * scalar);
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
}
