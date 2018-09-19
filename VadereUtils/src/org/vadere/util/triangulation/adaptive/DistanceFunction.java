package org.vadere.util.triangulation.adaptive;

import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.IDistanceFunction;

import java.util.Collection;

public class DistanceFunction implements IDistanceFunction {

	private final VRectangle regionBoundingBox;
	private final Collection<? extends VShape> obstacles;

	public DistanceFunction(final VRectangle regionBoundingBox, final Collection<? extends VShape> obstacles) {
		this.regionBoundingBox = regionBoundingBox;
		this.obstacles = obstacles;
	}

	@Override
	public Double apply(final IPoint iPoint) {
		double value = regionBoundingBox.distance(iPoint);
		for (VShape obstacle : obstacles) {
			value = doDDiff(value, obstacle.distance(iPoint));
		}
		return value;
	}
}
