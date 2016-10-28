package org.vadere.simulator.models.potential.fields;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.TargetPedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.math.InterpolationUtil;
import org.vadere.util.potential.CellGrid;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractPotentialFieldTarget implements IPotentialTargetGrid {

	protected static double EPSILON_SIM_TIME = 1e-100; // TODO [priority=medium] [task=fix] 1e-100 comparisons with values that are O(1e-8) are dangerous. Better use 1e-8 here.
	protected double lastUpdateTimestamp;
	private Topography topography;
	private boolean wasUpdated;
	private static Logger logger = LogManager.getLogger(AbstractPotentialFieldTarget.class);

	/**
	 * Stores all potential fields which represent the observation area. The key
	 * of the outer map equals the id of the floor, the key of the inner map
	 * equals the id of the target (there is a floor field per target).
	 */
	protected final HashMap<Integer, PotentialFieldAndInitializer> targetPotentialFields;

	public AbstractPotentialFieldTarget(final Topography topography) {
		this.topography = topography;
		this.wasUpdated = false;
		this.targetPotentialFields = new HashMap<>();
	}

	/**
	 *
	 * Returns the potential value of the static or dynamic target floor field.
	 * This does not take obstacle repulsion and pedestrian repulsion into
	 * account. This is specific to pedestrians. See pedestrian perception for
	 * more information.
	 *
	 */
	@Override
	public double getTargetPotential(final VPoint pos, final Agent ped) {
		
		if(!ped.hasNextTarget())
		{
			return 0.0;
		}

		CellGrid potentialField;
		double targetPotential = Double.MAX_VALUE;

		int targetId = ped.getNextTargetId();

		// Pedestrian has reached the target
		// TODO Is this necessary? The target controller changes the
		// pedestrian's target as soon the pedestrian arrives it.
		if (topography.getTarget(targetId).getShape().contains(pos)) {
			return 0; // the arrival time is zero
		}

		// Pedestrain inside an obstacle
		for (ScenarioElement b : topography.getObstacles()) {
			if (b.getShape().contains(pos)) {
				return Double.MAX_VALUE;
			}
		}

		/* Find minimal potential of given targets. */
		Optional<PotentialFieldAndInitializer> optionalPotentialFieldAndAnalyser =
				getPotentialFieldAndInitializer(targetId);

		// no target exist
		if (!optionalPotentialFieldAndAnalyser.isPresent()) {
			return 0;
		}

		PotentialFieldAndInitializer potentialFieldAndAnalyser = optionalPotentialFieldAndAnalyser.get();
		AttributesFloorField attributesFloorField = potentialFieldAndAnalyser.attributesFloorField;
		potentialField = potentialFieldAndAnalyser.eikonalSolver.getPotentialField();

		Point gridPoint = potentialField.getNearestPointTowardsOrigin(pos);
		VPoint gridPointCoord = potentialField.pointToCoord(gridPoint);
		int incX = 1, incY = 1;
		double gridPotentials[];
		double weightOfKnown[] = new double[1];
		double tmpPotential;

		if (pos.x >= potentialField.getWidth()) {
			incX = 0;
		}

		if (pos.y >= potentialField.getHeight()) {
			incY = 0;
		}

		List<Point> points = new LinkedList<>();
		points.add(gridPoint);
		points.add(new Point(gridPoint.x + incX, gridPoint.y));
		points.add(new Point(gridPoint.x + incX, gridPoint.y + incY));
		points.add(new Point(gridPoint.x, gridPoint.y + incY));
		gridPotentials = potentialFieldAndAnalyser.getGridPotentials(points);

		/* Interpolate the known (potential < Double.MAX_VALUE) values. */
		tmpPotential = InterpolationUtil
				.bilinearInterpolationWithUnkown(
						gridPotentials,
						(pos.x - gridPointCoord.x)
								/ potentialField.getResolution(),
						(pos.y - gridPointCoord.y)
								/ potentialField.getResolution(),
						weightOfKnown);

		/*
		 * If at least one node is known, a specialized version of
		 * interpolation is used: If the divisor weightOfKnown[ 0 ] would
		 * not be part of the equation, it would be a general bilinear
		 * interpolation using obstacleGridPenalty for the unknown. However,
		 * as soon as the interpolated value is not on the line of known
		 * values (weightOfKnown < 1) the potential is increased, like an
		 * additional penalty. The more the interpolated value moves into
		 * direction of the unknown, the higher the penalty becomes.
		 */
		if (weightOfKnown[0] > 0.00001) {
			tmpPotential = tmpPotential / weightOfKnown[0]
					+ (1 - weightOfKnown[0])
							* attributesFloorField.getObstacleGridPenalty();
		} else /* If all values are maximal, set potential to maximum. */
		{
			tmpPotential = Double.MAX_VALUE;
		}

		tmpPotential *= attributesFloorField.getTargetAttractionStrength();

		if (tmpPotential < targetPotential) {
			targetPotential = tmpPotential;
		}


		return targetPotential;
	}

	/**
	 * Updates all potential fields, for potentialfields for pedestrain- or moving targets this
	 * means that
	 * the PotentialFieldAndInitializer will be completely recreated.
	 *
	 * @param simTimeInSec
	 * @param target
	 * @param targetShapes
	 */
	protected void updatePotentialField(final double simTimeInSec, final Target target,
			final List<VShape> targetShapes) {
		if (target.isTargetPedestrian()) {
			if (!((TargetPedestrian) target).isDeleted()) {
				createNewPotentialFieldAndInitializer(target.getId(), targetShapes);
			}
		} else if (target.isMovingTarget()) {
			createNewPotentialFieldAndInitializer(target.getId(), targetShapes);
		}

		if (targetPotentialFields.containsKey(target.getId())) {
			targetPotentialFields.get(target.getId()).eikonalSolver.update();
		} else {
			logger.warn("potential field for target " + target.getId() + " is not contained in " + this);
		}
	}

	protected abstract void createNewPotentialFieldAndInitializer(final int targetId, final List<VShape> shapes);

	protected abstract boolean isNeedsUpdate(final double simTimeInSec);

	/**
	 * Indicate that update was called at least once!
	 * 
	 * @return
	 */
	protected boolean wasUpdated() {
		return wasUpdated;
	}

	protected Map<Integer, PotentialFieldAndInitializer> getPotentialFieldMap() {
		return targetPotentialFields;
	}

	@Override
	public void update(final double simTimeInSec) {

		List<Target> targets = topography.getTargets();
		Map<Integer, List<VShape>> mergeMap = topography.getTargetShapes();

		// update if necessary
		if (isNeedsUpdate(simTimeInSec)) {
			this.lastUpdateTimestamp = simTimeInSec;
			this.wasUpdated = true;
			targets.stream()
					.forEach(target -> updatePotentialField(simTimeInSec, target, mergeMap.get(target.getId())));
		}
	}

	/**
	 * Returns the secondaryPotential fields (which represents the dynamic
	 * potential fields) if they are available, otherwise the static potential
	 * fields.
	 *
	 * @return the secondaryPotential fields (which represents the dynamic
	 *         potential fields) if they are available, otherwise the static
	 *         potential fields.
	 */
	@Override
	public HashMap<Integer, CellGrid> getCellGrids() {
		HashMap<Integer, CellGrid> map = new HashMap<>();


		for (Map.Entry<Integer, PotentialFieldAndInitializer> entry2 : targetPotentialFields
				.entrySet()) {
			map.put(entry2.getKey(), entry2.getValue().eikonalSolver.getPotentialField());
		}

		return map;
	}

	protected Optional<PotentialFieldAndInitializer> getPotentialFieldAndInitializer(final int targetId) {
		if (targetPotentialFields.containsKey(targetId)) {
			return Optional.of(targetPotentialFields.get(targetId));
		} else {
			return Optional.empty();
		}
	}
}
