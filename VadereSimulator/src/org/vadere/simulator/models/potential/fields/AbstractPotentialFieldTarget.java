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
import org.vadere.util.potential.CellGrid;
import org.vadere.util.potential.calculators.EikonalSolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public abstract class AbstractPotentialFieldTarget implements IPotentialFieldTargetGrid {

	protected static double EPSILON_SIM_TIME = 1e-100; // TODO [priority=medium] [task=fix] 1e-100 comparisons with values that are O(1e-8) are dangerous. Better use 1e-8 here.
	protected double lastUpdateTimestamp;
	protected Topography topography;
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
     * Returns the potential value of the static or dynamic target potential field.
     * This does not take obstacle repulsion and pedestrian repulsion into
     * account. This is specific to agents. See pedestrian perception for more information.
     * The target potential does not exists (i.e. is equal to 0) if the agent has reached
     * his target (geometrically) or the agent does not have a next target or there is no
     * target potential field computed for the target of the agent (topographyError!).
     *
     * @param pos   the position for which the potential will be evaluated
     * @param agent the agent for which the potential will be evaluated
     * @return the target potential of the agent at position pos if it exists, and 0 otherwise.
     */
	@Override
	public double getPotential(final VPoint pos, final Agent agent) {
		
		if(!agent.hasNextTarget()) {
			return 0.0;
		}

		int targetId = agent.getNextTargetId();

		// the agent has reached his current target
		// TODO: expensive operation
		/*if (topography.getTarget(targetId).getShape().contains(pos)) {
			return 0.0;
		}*/

		// the agent is inside an obstacle
		// TODO: expensive operation
		/*for (ScenarioElement b : topography.getObstacles()) {
			if (b.getShape().contains(pos)) {
				return Double.MAX_VALUE;
			}
		}*/

		/* Find minimal potential of given targets. */
		Optional<PotentialFieldAndInitializer> optionalPotentialFieldAndAnalyser = getPotentialFieldAndInitializer(targetId);

		// no target exist
		if (!optionalPotentialFieldAndAnalyser.isPresent()) {
			logger.error("no target potential field for target = " + targetId + ", was found!");
			return 0.0;
		}

		PotentialFieldAndInitializer potentialFieldAndAnalyser = optionalPotentialFieldAndAnalyser.get();
		AttributesFloorField attributesFloorField = potentialFieldAndAnalyser.attributesFloorField;
		return potentialFieldAndAnalyser.eikonalSolver.getPotential(pos, attributesFloorField.getObstacleGridPenalty(), attributesFloorField.getTargetAttractionStrength());
	}


    @Override
    public IPotentialField copyFields() {
        final Map<Integer, Function<VPoint, Double>> map = new HashMap<>();
        for (Map.Entry<Integer, PotentialFieldAndInitializer> entry : targetPotentialFields.entrySet()) {
            AttributesFloorField attributes = entry.getValue().attributesFloorField;
            EikonalSolver eikonalSolver = entry.getValue().eikonalSolver;
            map.put(entry.getKey(), eikonalSolver.getSolution(attributes.getObstacleGridPenalty(), attributes.getTargetAttractionStrength()));
        }
        return (pos, agent) -> map.get(agent.getNextTargetId()).apply(pos);
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
