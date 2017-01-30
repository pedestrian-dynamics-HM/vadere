package org.vadere.simulator.models.potential.fields;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.TargetPedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.calculators.AbstractGridEikonalSolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractGridPotentialFieldTarget implements IPotentialTargetGrid {

	protected static double EPSILON_SIM_TIME = 1e-100; // TODO [priority=medium] [task=fix] 1e-100 comparisons with values that are O(1e-8) are dangerous. Better use 1e-8 here.
	protected double lastUpdateTimestamp;
	private Topography topography;
	private boolean wasUpdated;
	private static Logger logger = LogManager.getLogger(AbstractGridPotentialFieldTarget.class);

	/**
	 * Stores all potential fields which represent the observation area. The key
	 * of the outer map equals the id of the floor, the key of the inner map
	 * equals the id of the target (there is a floor field per target).
	 */
	protected final HashMap<Integer, PotentialFieldAndInitializer<? extends AbstractGridEikonalSolver>> targetPotentialFields;

	public AbstractGridPotentialFieldTarget(final Topography topography) {
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
		Optional<PotentialFieldAndInitializer<? extends AbstractGridEikonalSolver>> optionalPotentialFieldAndAnalyser =
				getPotentialFieldAndInitializer(targetId);

		// no target exist
		if (!optionalPotentialFieldAndAnalyser.isPresent()) {
			return 0;
		}

		return optionalPotentialFieldAndAnalyser.get().eikonalSolver.getValue(pos);
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

	protected Map<Integer, PotentialFieldAndInitializer<? extends AbstractGridEikonalSolver>> getPotentialFieldMap() {
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


		for (Map.Entry<Integer, PotentialFieldAndInitializer<? extends AbstractGridEikonalSolver>> entry2 : targetPotentialFields
				.entrySet()) {
			map.put(entry2.getKey(), entry2.getValue().eikonalSolver.getPotentialField());
		}

		return map;
	}

	protected Optional<PotentialFieldAndInitializer<? extends AbstractGridEikonalSolver>> getPotentialFieldAndInitializer(final int targetId) {
		if (targetPotentialFields.containsKey(targetId)) {
			return Optional.of(targetPotentialFields.get(targetId));
		} else {
			return Optional.empty();
		}
	}
}
