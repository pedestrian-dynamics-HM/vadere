package org.vadere.util.math;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.data.cellgrid.CellGrid;

import java.util.Collection;

/**
 * @author Benedikt Zoennchen
 */
public class DistanceFunctionTarget implements IDistanceFunction {

	private final CellGrid cellGrid;
	private final Collection<VShape> targets;

	public DistanceFunctionTarget(@NotNull final CellGrid cellGrid, @NotNull Collection<VShape> targets) {
		this.cellGrid = cellGrid;
		this.targets = targets;
	}

	@Override
	public Double apply(IPoint point) {
		/*VPoint dp = new VPoint(cellGrid.getWidth() / (cellGrid.getNumPointsX() - 1) / 2.0,
				cellGrid.getHeight() / (cellGrid.getNumPointsY() - 1) / 2.0);*/
		double dist = Double.MAX_VALUE;
		for (VShape targetShape : targets) {
			// negative distances are possible when point is inside the target
			dist = Math.min(dist, targetShape.distance(point));
		}
		return -dist;
	}
}
