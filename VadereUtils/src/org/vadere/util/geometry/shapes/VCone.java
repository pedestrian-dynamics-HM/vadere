package org.vadere.util.geometry.shapes;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.GeometryUtils;

/**
 * @author Benedikt Zoennchen
 */
public class VCone {
	private VPoint position;
	private VPoint direction;
	private double angle;

	public VCone(@NotNull final VPoint position, @NotNull final VPoint direction, double angle) {
		if(angle <= 0) {
			throw new IllegalArgumentException("angle of a cone has to be greater than 0.");
		}

		this.position = position;
		this.direction = direction.norm();
		this.angle = angle;
	}

	public boolean contains(final IPoint point) {
		double angle = GeometryUtils.angle(point, position, position.add(direction));
		return angle <= this.angle / 2;
	}

	public boolean overlapLineSegment(final VLine line) {
		VPoint a = new VPoint(line.getX1(), line.getY1());
		VPoint b = new VPoint(line.getX2(), line.getY2());

		if(contains(a) ||contains(b)) {
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
}
