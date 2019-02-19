package org.vadere.util.math;


import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;


/**
 * @author Benedikt Zoennchen
 *
 * A signed distance function d.
 * d(x) greater than 0 then x is outside
 * d(x) smaller or equals 0 then x is inside
 */
@FunctionalInterface
public interface IDistanceFunction extends Function<IPoint, Double> {

	static IDistanceFunction createRing(final double xCenter, final double yCenter, final double innerRadius, final double outerRadius) {
		final double x1 = (outerRadius + innerRadius) / 2.0;
		final double x2 = (outerRadius - innerRadius) / 2.0;
		return p -> {
			double dx = p.getX() - xCenter;
			double dy = p.getY() - yCenter;
			double len = Math.sqrt(dx * dx + dy * dy);
			return Math.abs(len - x1) - x2;
		};
	}

	static IDistanceFunction createDisc(final double xCenter, final double yCenter, final double radius) {
		return p -> {
			double dx = p.getX() - xCenter;
			double dy = p.getY() - yCenter;
			return Math.sqrt(dx * dx + dy * dy) - radius;
		};
	}

	static IDistanceFunction create(final VRectangle regionBoundingBox, final Collection<? extends VShape> obstacles) {
		return new DistanceFunction(regionBoundingBox, obstacles);
	}

	static IDistanceFunction create(final VRectangle regionBoundingBox, final VShape ... shapes) {
		List<VShape> shapeList = new ArrayList<>();
		for(VShape shape : shapes) {
			shapeList.add(shape);
		}
		return new DistanceFunction(regionBoundingBox, shapeList);
	}

	static IDistanceFunction create(final VPolygon regionBoundingBox, final VShape ... shapes) {
		List<VShape> shapeList = new ArrayList<>();
		for(VShape shape : shapes) {
			shapeList.add(shape);
		}
		return new DistanceFunction(regionBoundingBox, shapeList);
	}

	static IDistanceFunction union(final IDistanceFunction dist1, final IDistanceFunction dist2) {
		return p -> Math.min(dist1.apply(p), dist2.apply(p));
	}

	static IDistanceFunction intersect(final IDistanceFunction dist1, final IDistanceFunction dist2) {
		return p -> Math.max(dist1.apply(p), dist2.apply(p));
	}

	static IDistanceFunction substract(final IDistanceFunction dist1, final IDistanceFunction dist2) {
		return p -> Math.max(dist1.apply(p), -dist2.apply(p));
	}

	default double doDDiff(double d1, double d2)
	{
		return Math.max(d1, -d2);
	}

	default double doDUnion(double d1, double d2) {
		return Math.min(d1, d2);
	}

}
