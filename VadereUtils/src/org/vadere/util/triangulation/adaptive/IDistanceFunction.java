package org.vadere.util.triangulation.adaptive;


import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Collection;
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

	default double doDDiff(double d1, double d2)
	{
		return Math.max(d1, -d2);
	}

	default double doDUnion(double d1, double d2) {
		return Math.min(d1, d2);
	}

}
