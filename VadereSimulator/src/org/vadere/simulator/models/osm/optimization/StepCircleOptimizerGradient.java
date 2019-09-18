package org.vadere.simulator.models.osm.optimization;

import java.awt.Shape;
import java.util.LinkedList;
import java.util.List;

import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTargetGrid;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.simulator.models.potential.solver.gradients.FloorGradientProviderDiscrete;
import org.vadere.simulator.models.potential.solver.gradients.GradientProvider;

public class StepCircleOptimizerGradient extends StepCircleOptimizer {

	private final AttributesOSM attributesOSM;
	private final IPotentialFieldTargetGrid potentialFieldTarget;
	private final List<Integer> targetIds;
	private final Topography topography;

	public StepCircleOptimizerGradient(Topography topography,
			IPotentialFieldTargetGrid potentialFieldTarget,
			AttributesOSM attributesOSM) {

		this.attributesOSM = attributesOSM;
		this.topography = topography;

		this.targetIds = new LinkedList<>();
		for (Target target : topography.getTargets()) {
			targetIds.add(target.getId());
		}

		this.potentialFieldTarget = potentialFieldTarget;
	}

	@Override
	public VPoint getNextPosition(PedestrianOSM ped, Shape reachableArea) {

		double stepSize = ((VCircle) reachableArea).getRadius();

		GradientProvider floorGradient = new FloorGradientProviderDiscrete(
				potentialFieldTarget.getCellGrids(),
				topography.getBounds(), targetIds);

		VPoint position = ped.getPosition();

		// TODO [priority=low] [task=refactoring] should be provided by PotentialFieldTarget
		double[] targetGrad = new double[2];
		double[] arrayPos = {position.x, position.y};

		int targetId = ped.getNextTargetId();
		floorGradient.gradient(0, targetId, arrayPos, targetGrad);
		Vector2D directionTarget = new Vector2D(targetGrad[0], targetGrad[1]);

		Vector2D gradientObstacle = ped.getObstacleGradient(position);
		Vector2D gradientPedestrians = ped.getPedestrianGradient(position);
		Vector2D directionDynamic = gradientObstacle.add(gradientPedestrians);
		Vector2D direction = directionTarget.add(directionDynamic).normalize(1.0);

		double resolution = stepSize / attributesOSM.getStepCircleResolution();

		VPoint result = position;
		double resultPot = ped.getPotential(position);
		VPoint nextPos;
		double nextPot;

		for (int i = 0; i <= attributesOSM.getStepCircleResolution(); i++) {

			VPoint nextMove = direction.normalize(resolution * i);
			nextPos = new VPoint(position.x - nextMove.x, position.y
					- nextMove.y);

			nextPot = ped.getPotential(nextPos);

			if (resultPot > nextPot) {
				result = nextPos;
				resultPot = nextPot;
			}
		}

		return result;
	}

	public StepCircleOptimizer clone() {
		return new StepCircleOptimizerGradient(topography, potentialFieldTarget, attributesOSM);
	}
}
