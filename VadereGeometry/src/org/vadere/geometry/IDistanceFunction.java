package org.vadere.geometry;


import org.vadere.geometry.shapes.IPoint;
import org.vadere.geometry.shapes.VRectangle;
import org.vadere.geometry.shapes.VShape;
import org.vadere.geometry.mesh.triangulation.adaptive.DistanceFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;


/**
 * @author Benedikt Zoennchen
 *
 * A signed distance function d.
 * d(x) > 0 => x is outside
 * d(x) <= 0 => x is inside
 */
@FunctionalInterface
public interface IDistanceFunction extends Function<IPoint, Double> {

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

	static IDistanceFunction union(final IDistanceFunction dist1, final IDistanceFunction dist2) {
		return p -> Math.min(dist1.apply(p), dist2.apply(p));
	}

	static IDistanceFunction intersect(final IDistanceFunction dist1, final IDistanceFunction dist2) {
		return p -> Math.max(dist1.apply(p), dist2.apply(p));
	}

	default double doDDiff(double d1, double d2)
	{
		return Math.max(d1, -d2);
	}

	default double doDUnion(double d1, double d2) {
		return Math.min(d1, d2);
	}

}
