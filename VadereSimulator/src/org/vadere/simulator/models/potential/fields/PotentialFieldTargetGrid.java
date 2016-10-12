package org.vadere.simulator.models.potential.fields;

import java.util.*;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.*;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

/**
 * The default IPotentialTargetGrid, that creates for each target the same floor field type
 * based on the AttributesFloorField.
 *
 */
public class PotentialFieldTargetGrid<T extends Agent> extends AbstractPotentialFieldTarget {

	// private HashMap<Integer, PotentialFieldAndInitializer> staticPotentialFields;
	/** The topography the floor fields are generated for. */
	private Topography topography;

	/* Optimization */
	private boolean potentialFieldsNeedUpdate;
	private AttributesFloorField attributes;
	private AttributesAgent attributesPedestrian;

	public PotentialFieldTargetGrid(
			final Topography topography,
			final AttributesAgent attributesPedestrian,
			final AttributesFloorField attributesPotential) {
		super(topography);
		this.topography = topography;
		this.attributesPedestrian = attributesPedestrian;
		this.potentialFieldsNeedUpdate = false;
		this.lastUpdateTimestamp = -1;

		this.attributes = attributesPotential;
	}

	@Override
	protected boolean isNeedsUpdate(final double simTimeInSec) {
		return (potentialFieldsNeedUpdate
				|| topography.containsTarget(t -> (t.isMovingTarget() || t.isTargetPedestrian())))
				&& Math.abs(lastUpdateTimestamp - simTimeInSec) > EPSILON_SIM_TIME;
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
		createMissingPotentialFieldAndInitializers();
	}

	private void createMissingPotentialFieldAndInitializers() {
		Map<Integer, List<VShape>> mergeMap = topography.getTargetShapes();
		topography.getTargets().stream().filter(t -> !getPotentialFieldAndInitializer(t.getId()).isPresent())
				.forEach(t -> createNewPotentialFieldAndInitializer(t.getId(), mergeMap.get(t.getId())));
	}

	@Override
	protected void createNewPotentialFieldAndInitializer(final int targetId, final List<VShape> shapes) {
		PotentialFieldAndInitializer potentialFieldAndInitializer = PotentialFieldAndInitializer.create(topography,
				targetId, shapes, this.attributesPedestrian, this.attributes);
		potentialFieldsNeedUpdate =
				potentialFieldsNeedUpdate || potentialFieldAndInitializer.eikonalSolver.needsUpdate();
		targetPotentialFields.put(targetId, potentialFieldAndInitializer);
	}

	@Override
	public void postLoop(final double simTimeInSec) {}

	@Override
	public void initialize(List<Attributes> attributesList, Topography topography,
			AttributesAgent attributesPedestrian, Random random) {
		// TODO should be used to initialize the Model
	}
}
