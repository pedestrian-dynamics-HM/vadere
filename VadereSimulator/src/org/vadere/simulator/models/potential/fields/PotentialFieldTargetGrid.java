package org.vadere.simulator.models.potential.fields;

import java.util.*;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.*;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.potential.calculators.EikonalSolver;

/**
 * The default IPotentialTargetGrid, that creates for each target the same floor field type
 * based on the AttributesFloorField.
 *
 */
public class PotentialFieldTargetGrid extends APotentialFieldTargetGrid {

    /**
     * false if and only if there exits no dynamic potential field.
     */
	private boolean potentialFieldsNeedUpdate;

    /**
     * configuration of the potential fields.
     */
	private AttributesFloorField attributes;

    /**
     * configuration of the agents.
     */
	private AttributesAgent attributesPedestrian;

	public PotentialFieldTargetGrid(
			final Topography topography,
			final AttributesAgent attributesPedestrian,
			final AttributesFloorField attributesPotential) {
		super(topography);
		this.attributesPedestrian = attributesPedestrian;
		this.potentialFieldsNeedUpdate = false;
		this.lastUpdateTimestamp = -1;
		this.attributes = attributesPotential;
	}

    /**
     * Returns true if this potential field has to be updated which is the case if:
     *
     * the simulation state changed (i.e. the simulation time) and
     * - there exists any dynamic potential field or
     * - there exists any moving target or
     * - there exits any agent which is a target himself (helping behaviour)
     *
     * @param simTimeInSec the current simulation time in seconds
     * @return
     */
	@Override
	protected boolean isNeedsUpdate(final double simTimeInSec) {
	    return super.isNeedsUpdate(simTimeInSec) &&
                (potentialFieldsNeedUpdate || topography.containsTarget(t -> (t.isMovingTarget() || t.isTargetPedestrian())));
	}

	@Override
	public boolean needsUpdate() {
		return potentialFieldsNeedUpdate;
	}


	@Override
	public Vector2D getTargetPotentialGradient(VPoint pos, Agent ped) {
		throw new UnsupportedOperationException("gradient not yet implemented");
	}

    @Override
	public void preLoop(final double simTimeInSec) {
		addMissingEikonalSolvers();
	}

	private void addMissingEikonalSolvers() {
		Map<Integer, List<VShape>> mergeMap = topography.getTargetShapes();
		topography.getTargets().stream()
                .filter(t -> !getSolver(t.getId()).isPresent())
				.forEach(t -> addEikonalSolver(t.getId(), mergeMap.get(t.getId())));
	}

	@Override
	protected void addEikonalSolver(final int targetId, final List<VShape> shapes) {
		EikonalSolver eikonalSolver = IPotentialField.create(topography, targetId, shapes, attributesPedestrian, attributes);
		potentialFieldsNeedUpdate = potentialFieldsNeedUpdate || eikonalSolver.needsUpdate();
		eikonalSolvers.put(targetId, eikonalSolver);
	}

	@Override
	public void postLoop(final double simTimeInSec) {}

	@Override
	public void initialize(List<Attributes> attributesList, Topography topography, AttributesAgent attributesPedestrian, Random random) {}

}
