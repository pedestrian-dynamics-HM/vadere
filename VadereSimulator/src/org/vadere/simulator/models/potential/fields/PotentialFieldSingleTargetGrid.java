package org.vadere.simulator.models.potential.fields;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.potential.calculators.EikonalSolver;

/**
 * A IPotentialTargetGrid, that creates for a single target the a floor field
 * based on the AttributesFloorField.
 *
 */
public class PotentialFieldSingleTargetGrid extends AbstractGridPotentialFieldTarget {

	private Topography topography;
	private AttributesFloorField attributesFloorField;
	private AttributesAgent attributesPedestrian;
	private final int targetId;

	public PotentialFieldSingleTargetGrid(final Topography topography,
			final AttributesAgent attributesPedestrian,
			final AttributesFloorField attributesPotential,
			final int targetId) {
		super(topography);
		this.attributesFloorField = attributesPotential;
		this.topography = topography;
		this.attributesPedestrian = attributesPedestrian;
		this.targetId = targetId;
	}

	@Override
	protected boolean isNeedsUpdate(double simTimeInSec) {
		return needsUpdate() && Math.abs(lastUpdateTimestamp - simTimeInSec) > EPSILON_SIM_TIME;
	}

	@Override
	public boolean needsUpdate() {
		return (getSolver(targetId).isPresent()
				&& getSolver(targetId).get().needsUpdate())
				|| topography.containsTarget(t -> t.isMovingTarget() || t.isTargetPedestrian(), targetId);
	}

	@Override
	public Vector2D getTargetPotentialGradient(VPoint pos, Agent ped) {
		throw new UnsupportedOperationException("method not jet implemented.");
	}

	@Override
	public void preLoop(double simTimeInSec) {
		if (!getSolver(targetId).isPresent()) {
			List<Target> targets = topography.getTargets(targetId);
			List<VShape> shapes = targets.stream().map(t -> t.getShape()).collect(Collectors.toList());
			createNewPotentialFieldAndInitializer(targetId, shapes);
		}
	}

	@Override
	protected void createNewPotentialFieldAndInitializer(final int targetId, final List<VShape> shapes) {
		if (targetId == this.targetId) {
			EikonalSolver eikonalSolver = IPotentialField.create(topography,
					targetId, shapes, this.attributesPedestrian, this.attributesFloorField);
			targetPotentialFields.put(targetId, eikonalSolver);
		}
	}

	@Override
	public void postLoop(double simTimeInSec) {}

	@Override
	public void initialize(List<Attributes> attributesList, Topography topography,
			AttributesAgent attributesPedestrian, Random random) {
		// TODO should be used to initialize the Model
	}

}
