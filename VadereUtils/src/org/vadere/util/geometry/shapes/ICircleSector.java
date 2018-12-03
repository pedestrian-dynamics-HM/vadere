package org.vadere.util.geometry.shapes;

import com.google.common.collect.ImmutableList;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author Benedikt Zoennchen
 */
public interface ICircleSector {
	boolean contains(VPoint point);

	default ImmutableList<VPoint> getIntersectionPoints(@NotNull final VLine line) {
		return getIntersectionPoints(line.getX1(), line.getY1(), line.getX2(), line.getY2());
	}

	default ImmutableList<VPoint> getIntersectionPoints(@NotNull final VPoint p, @NotNull final VPoint q) {
		return getIntersectionPoints(p.getX(), p.getY(), q.getX(), q.getY());
	}

	ImmutableList<VPoint> getIntersectionPoints(final double x1, final double y1, final double x2, final double y2);

	default Optional<VPoint> getClosestIntersectionPoint(@NotNull final VLine line, @NotNull final VPoint r) {
		return getClosestIntersectionPoint(line.getX1(), line.getY1(), line.getX2(), line.getY2(), r.getX(), r.getY());
	}

	default Optional<VPoint> getClosestIntersectionPoint(@NotNull final VPoint p, @NotNull final VPoint q, @NotNull final VPoint r) {
		Optional<VPoint> result = getClosestIntersectionPoint(p.getX(), p.getY(), q.getX(), q.getY(), r.getX(), r.getY());
		return result;
	}

	Optional<VPoint> getClosestIntersectionPoint(final double x1, final double y1, final double x2, final double y2, final double x3, final double y3);

	default boolean intersectsLine(@NotNull VLine intersectingLine) {
		return intersectsLine(intersectingLine.getX1(), intersectingLine.getY1(), intersectingLine.getX2(), intersectingLine.getY2());
	}

	default boolean intersectsLine(@NotNull VPoint p, @NotNull VPoint q) {
		return intersectsLine(p.getX(), p.getY(), q.getX(), q.getY());
	}

	boolean intersectsLine(final double x1, final double y1, final double x2, final double y2);

	double getRadius();

	VPoint getCenter();
}
