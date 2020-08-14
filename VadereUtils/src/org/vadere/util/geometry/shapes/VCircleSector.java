package org.vadere.util.geometry.shapes;

import com.google.common.collect.ImmutableList;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.GeometryUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * @author Benedikt Zoennchen
 *
 * A VCircleSector is a part of a disc. The disc is defined by the center and the radius and the
 * part by the minimum and maximum angle3D.
 */
public class VCircleSector implements ICircleSector {

	private final VPoint center;
	private final double radius;
	private final double minAngle;
	private final double maxAngle;
	private final VCircle circle;
	private VLine line1;
	private VLine line2;

	public VCircleSector(@NotNull final VPoint center, final double radius, final double minAngle, final double maxAngle) {
		assert minAngle < maxAngle : minAngle + "<" + maxAngle;
		assert minAngle >= 0 && minAngle < 2 * Math.PI;
		assert maxAngle > 0 && maxAngle <= 2 * Math.PI;
		assert !(minAngle <= 0 && maxAngle >= 2 * Math.PI); // circle!

		this.radius = radius;
		this.center = center;
		this.minAngle = minAngle;
		this.maxAngle = maxAngle;
		this.circle = new VCircle(center, radius);
	}

	public double getRadius() {
		return this.radius;
	}

	public VPoint getCenter() {
		return center;
	}

	@Override
	public boolean contains(VPoint point) {
		return false;
	}

	@Override
	public ImmutableList<VPoint> getIntersectionPoints(double x1, double y1, double x2, double y2) {

		List<VPoint> intersectionPoints = new ArrayList<>();
		if (getLine1().intersectsLine(x1, y1, x2, y2)) {
			intersectionPoints.add(GeometryUtils.lineIntersectionPoint(getLine1(), x1, y1, x2, y2));
		}

		if (getLine2().intersectsLine(x1, y1, x2, y2)) {
			intersectionPoints.add(GeometryUtils.lineIntersectionPoint(getLine2(), x1, y1, x2, y2));
		}

		circle.getIntersectionPoints(x1, y1, x2, y2).stream().filter(p -> insideAngle(GeometryUtils.angleTo(getCenter(), p))).forEach(p -> intersectionPoints.add(p));

		return ImmutableList.copyOf(intersectionPoints);
	}

	private boolean insideAngle(double angle) {
		return angle >= minAngle && angle <= maxAngle;
	}

	@Override
	public Optional<VPoint> getClosestIntersectionPoint(double x1, double y1, double x2, double y2, double x3, double y3) {
		return getIntersectionPoints(x1, y1, x2, y2).stream().min(Comparator.comparingDouble(p -> p.distance(x3, y3)));
	}

	@Override
	public boolean intersectsLine(double x1, double y1, double x2, double y2) {
		if (getLine1().intersectsLine(x1, y1, x2, y2) || getLine2().intersectsLine(x1, y1, x2, y2)) {
			return true;
		} else {
			// intersection points with circle
			ImmutableList<VPoint> interSectionPoints = circle.getIntersectionPoints(x1, y1, x2, y2);
			// is one of this intersection points inside the section
			return interSectionPoints.stream().anyMatch(p -> insideAngle(GeometryUtils.angleTo(getCenter(), p)));
		}
	}

	private VLine getLine1() {
		if (line1 == null) {
			line1 = new VLine(center.getX(), center.getY(), Math.cos(minAngle) * radius, Math.sin(minAngle) * radius);
		}
		return line1;
	}

	private VLine getLine2() {
		if (line2 == null) {
			line2 = new VLine(center.getX(), center.getY(), Math.cos(maxAngle) * radius, Math.sin(maxAngle) * radius);
		}
		return line2;
	}

}
