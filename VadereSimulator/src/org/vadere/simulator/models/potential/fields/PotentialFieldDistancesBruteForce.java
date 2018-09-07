package org.vadere.simulator.models.potential.fields;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.scenario.Agent;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.CellState;
import org.vadere.util.potential.PathFindingTag;
import java.awt.*;
import java.util.Collection;

/**
 * @author Benedikt Zoennchen
 *
 * PotentialFieldDistanceEikonalEq computes the nearest distnace to any obstacle by computing
 * the distance at certain discrete points lying on an Cartesian grid. Values inbetween are
 * bilinear interpolated. To compute the distance at these grid points the the exact distances
 * to all obstacles are computed choosing the minimum.
 *
 * Note: This can be computational expensive if there are many and or complex obstacles.
 */
public class PotentialFieldDistancesBruteForce implements IPotentialField {

	private static Logger logger = LogManager.getLogger(PotentialFieldDistancesBruteForce.class);
	private final CellGrid cellGrid;
	private final Collection<VShape> obstacles;

	public PotentialFieldDistancesBruteForce(@NotNull final Collection<VShape> obstacles,
									 @NotNull final VRectangle bounds,
									 @NotNull final AttributesFloorField attributesFloorField) {
		double ms = System.currentTimeMillis();
		this.obstacles = obstacles;
		this.cellGrid = new CellGrid(bounds.getWidth(), bounds.getHeight(), attributesFloorField.getPotentialFieldResolution(), new CellState());
		this.cellGrid.pointStream().forEach(p -> computeDistanceToGridPoint(p));
		logger.info("floor field initialization time:" + (System.currentTimeMillis() - ms + "[ms]"));
	}

	private void computeDistanceToGridPoint(@NotNull final Point gridPoint) {
		VPoint point = cellGrid.pointToCoord(gridPoint);
		double distance = obstacles.stream().map(shape -> shape.distance(point)).min(Double::compareTo).orElse(Double.MAX_VALUE);
		cellGrid.setValue(gridPoint, new CellState(distance, PathFindingTag.Reachable));
	}

	@Override
	public double getPotential(@NotNull VPoint pos, @Nullable Agent agent) {
		return cellGrid.getInterpolatedValueAt(pos).getLeft();
	}
}
