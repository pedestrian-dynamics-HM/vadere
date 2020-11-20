package org.vadere.util.geometry.shapes;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.GeometryUtils;

/**
 * @author Benedikt Zoennchen
 */
public class VCone {
	private VPoint rayDirection1;
	private VPoint rayDirection2;
	private VPoint position;
	private VPoint rayPosition1;
	private VPoint rayPosition2;
	private VPoint direction;
	private double angle;

	public VCone(@NotNull final VPoint position, @NotNull final VPoint direction, double angle) {
		if(angle <= 0 || angle >= Math.PI) {
			throw new IllegalArgumentException("angle3D of a cone has to be greater than 0 and smaller than pi.");
		}

		this.position = position;
		this.direction = direction.norm();
		this.rayDirection1 = direction.rotate(angle/2).norm();
		this.rayDirection2 = direction.rotate(-angle/2).norm();
		this.angle = angle;

	}

	public boolean contains(final VPoint point) {
		double angle1 = GeometryUtils.angleTo(point.subtract(position));
		double angle2 = GeometryUtils.angleTo(direction);
		return Math.abs(angle1 - angle2) < angle / 2;
	}

	/*public boolean intersect(final VTriangle triangle) {
		double bound = triangle.maxCoordinate();
		this.rayPosition1 = position.add(rayDirection1.scalarMultiply(bound));
		this.rayPosition2 = position.add(rayDirection2.scalarMultiply(bound));
		if(rayPosition1.equals(rayPosition2)) {
			System.out.println("ww");
		}
		return new VTriangle(position, rayPosition1, rayPosition2).intersect(triangle);
	}*/

	/*
	 * TODO: test
	 */
	public boolean overlapLineSegment(final VLine line) {
		VPoint a = new VPoint(line.getX1(), line.getY1());
		VPoint b = new VPoint(line.getX2(), line.getY2());

		if(contains(a) || contains(b)) {
			return false;
		}

		VPoint v1 = position.subtract(a);
		VPoint v2 = b.subtract(a);
		VPoint v3 = new VPoint(-direction.getY(), direction.getX());
		double t1 = v2.crossProduct(v1) / v2.scalarProduct(v3);
		double t2 = v1.scalarProduct(v3) / v2.scalarProduct(v3);
		assert Double.isFinite(t1) && Double.isFinite(t2);

		// the line segment from a to b intersect the cone?
		return t1 >= 0 && t2 <= 1 && t2 >= 0;
	}

	private boolean isLeft(IPoint a, IPoint b, IPoint c) {
		return ((b.getY() - a.getX())*(c.getY() - a.getY()) - (b.getY() - a.getY())*(c.getX() - a.getX())) > 0;
	}

	private boolean isRight(IPoint a, IPoint b, IPoint c) {
		return ((b.getY() - a.getX())*(c.getY() - a.getY()) - (b.getY() - a.getY())*(c.getX() - a.getX())) < 0;
	}

	private boolean isOn(IPoint a, IPoint b, IPoint c) {
		return ((b.getY() - a.getX())*(c.getY() - a.getY()) - (b.getY() - a.getY())*(c.getX() - a.getX())) == 0;
	}
}
