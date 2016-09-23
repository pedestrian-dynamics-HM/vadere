package org.vadere.simulator.models.potential.fields;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

/**
 * A IPotentialTargetGrid, that creates for each target another floor field type
 * based on the the Map<Integer, AttributesFloorField>.
 *
 *
 *         Not finiehsed!
 */
public class PotentialFieldMultiTargetGrid<T extends Agent> extends AbstractPotentialFieldTarget {

	private static double EPSILON_SIM_TIME = 1e-100;
	private final Map<Integer, AttributesFloorField> attributesByTarget;
	private double lastUpdateTimestamp;
	private final Topography topography;
	private boolean potentialFieldsNeedUpdate;
	private final AttributesAgent attributesPedestrian;

	public PotentialFieldMultiTargetGrid(final Topography topography, final AttributesAgent attributesPedestrian,
			final Map<Integer, AttributesFloorField> attributesByTarget) {
		super(topography);
		this.attributesByTarget = attributesByTarget;
		this.lastUpdateTimestamp = 0;
		this.topography = topography;
		this.attributesPedestrian = attributesPedestrian;

	}

	@Override
	protected void updatePotentialField(final double simTimeInSec, final Target target,
			final List<VShape> targetShapes) {
		lastUpdateTimestamp = simTimeInSec;
		targetPotentialFields.get(target.getId()).eikonalSolver.update();
	}

	@Override
	protected boolean isNeedsUpdate(double simTimeInSec) {
		return Math.abs(lastUpdateTimestamp - simTimeInSec) > EPSILON_SIM_TIME;
	}

	@Override
	public boolean needsUpdate() {
		return potentialFieldsNeedUpdate;
	}

	@Override
	public Vector2D getTargetPotentialGradient(VPoint pos, Agent ped) {
		throw new UnsupportedOperationException("method not jet implemented.");
	}

	@Override
	public void preLoop(double simTimeInSec) {
		createMissingPotentialFieldAndInitializers();
	}

	private void createMissingPotentialFieldAndInitializers() {
		Map<Integer, List<VShape>> mergeMap = topography.getTargetShapes();
		attributesByTarget.keySet().stream()
				.filter(targetId -> !getPotentialFieldAndInitializer(targetId).isPresent())
				.forEach(targetId -> createNewPotentialFieldAndInitializer(targetId, mergeMap.get(targetId)));
	}

	@Override
	protected void createNewPotentialFieldAndInitializer(final int targetId, final List<VShape> shapes) {
		PotentialFieldAndInitializer potentialFieldAndInitializer = PotentialFieldAndInitializer.create(topography,
				targetId, shapes, this.attributesPedestrian, attributesByTarget.get(targetId));
		potentialFieldsNeedUpdate =
				potentialFieldsNeedUpdate || potentialFieldAndInitializer.eikonalSolver.needsUpdate();
		targetPotentialFields.put(targetId, potentialFieldAndInitializer);
	}

	@Override
	public void postLoop(double simTimeInSec) {}

	@Override
	public void initialize(List<Attributes> attributesList, Topography topography,
			AttributesAgent attributesPedestrian, Random random) {
		// TODO should be used to initialize the Model
	}

}
