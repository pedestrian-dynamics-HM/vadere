package org.vadere.util.math;

import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Benedikt Zoennchen
 */
public class DistanceFunction implements IDistanceFunction {

	private final VShape regionBoundingBox;
	private final Collection<? extends VShape> obstacles;
	private final Collection<? extends VShape> targets;

	public DistanceFunction(final VRectangle regionBoundingBox, final Collection<? extends VShape> obstacles, final Collection<? extends VShape> targets) {
		this.regionBoundingBox = regionBoundingBox;
		this.obstacles = obstacles;
		this.targets = targets;
	}

	public DistanceFunction(final VRectangle regionBoundingBox, final Collection<? extends VShape> obstacles) {
		this(regionBoundingBox, obstacles, Collections.EMPTY_LIST);
	}

	public DistanceFunction(final VPolygon regionBoundingBox, final Collection<? extends VShape> obstacles, final Collection<? extends VShape> targets) {
		this.regionBoundingBox = regionBoundingBox;
		this.obstacles = obstacles;
		this.targets = targets;
	}

	public DistanceFunction(final VPolygon regionBoundingBox, final Collection<? extends VShape> obstacles) {
		this(regionBoundingBox, obstacles, Collections.EMPTY_LIST);
	}

	//return Math.max(d1, -d2);
	@Override
	public Double apply(final IPoint iPoint) {
		double value = regionBoundingBox.distance(iPoint);
		for (VShape obstacle : obstacles) {
			value = doDDiff(value, obstacle.distance(iPoint));
		}
		for (VShape target : targets) {
			value = doDDiff(value, Math.abs(target.distance(iPoint)));
		}
		return value;
	}
}

