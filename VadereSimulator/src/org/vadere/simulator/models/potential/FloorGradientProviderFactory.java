package org.vadere.simulator.models.potential;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.vadere.simulator.models.potential.fields.IPotentialFieldTargetGrid;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.GradientProviderType;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.potential.gradients.FloorGradientProviderDiscrete;
import org.vadere.util.potential.gradients.FloorGradientProviderEuclidean;
import org.vadere.util.potential.gradients.FloorGradientProviderEuclideanMollified;
import org.vadere.util.potential.gradients.GradientProvider;

/**
 * Factory for {@link GradientProvider}.
 * 
 * 
 */
public class FloorGradientProviderFactory {
	/**
	 * Creates a floor gradient provider based on {@link GradientProvider}.
	 * 
	 * @param type
	 *        the type of the floor gradient provider.
	 * @param attributes
	 *        scenario and target map.
	 * @return an appropriate floor gradient provider, or null
	 */
	public static GradientProvider createFloorGradientProvider(
			GradientProviderType type, Topography scenario,
			Map<Integer, Target> targets, IPotentialFieldTargetGrid potentialField) {
		GradientProvider result = null;

		// TODO [priority=low] [task=refactoring] refactor
		List<Integer> targetIds = new LinkedList<>();
		for (Target target : targets.values()) {
			targetIds.add(target.getId());
		}
		Map<Integer, VShape> targetShapesWithId = new HashMap<>();
		for (Target target : targets.values()) {
			targetShapesWithId.put(target.getId(), target.getShape());
		}
		List<VShape> obstacles = new LinkedList<>();
		for (Obstacle obstacle : scenario.getObstacles()) {
			obstacles.add(obstacle.getShape());
		}

		if (type == null) {
			type = GradientProviderType.FLOOR_EIKONAL_DISCRETE;
		}

		switch (type) {
			case FLOOR_EIKONAL_DISCRETE:
				result = new FloorGradientProviderDiscrete(
						potentialField.getCellGrids(),
						scenario.getBounds(), targetIds);
				break;
			case FLOOR_EUCLIDEAN_CONTINUOUS:
				result = new FloorGradientProviderEuclidean(targetShapesWithId);
				break;
			case FLOOR_EUCLIDEAN_CONTINUOUS_MOLLIFIED:
				result = new FloorGradientProviderEuclideanMollified(
						targetShapesWithId);
				break;
			default:
				break;
		}

		return result;
	}
}
