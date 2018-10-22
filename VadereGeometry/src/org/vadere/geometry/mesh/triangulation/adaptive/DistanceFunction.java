package org.vadere.geometry.mesh.triangulation.adaptive;

import org.vadere.geometry.IDistanceFunction;
import org.vadere.geometry.shapes.IPoint;
import org.vadere.geometry.shapes.VPolygon;
import org.vadere.geometry.shapes.VRectangle;
import org.vadere.geometry.shapes.VShape;

import java.util.Collection;

/**
 * @author Benedikt Zoennchen
 */
public class DistanceFunction implements IDistanceFunction {

	private final VShape regionBoundingBox;
	private final Collection<? extends VShape> obstacles;

	public DistanceFunction(final VRectangle regionBoundingBox, final Collection<? extends VShape> obstacles) {
		this.regionBoundingBox = regionBoundingBox;
		this.obstacles = obstacles;
	}

	public DistanceFunction(final VPolygon regionBoundingBox, final Collection<? extends VShape> obstacles) {
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
