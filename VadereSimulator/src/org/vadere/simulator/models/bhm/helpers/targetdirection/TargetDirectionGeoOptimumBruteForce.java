package org.vadere.simulator.models.bhm.helpers.targetdirection;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.bhm.PedestrianBHM;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.state.scenario.Target;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.util.Comparator;
import java.util.List;

public class TargetDirectionGeoOptimumBruteForce implements TargetDirection {
	private static final Logger logger = Logger.getLogger(TargetDirectionGeoOptimumBruteForce.class);
	private PedestrianBHM pedestrianBHM;
	private IPotentialFieldTarget targetPotentialField;
	private int nPoints = 100;
	private TargetDirection fallback;

	public TargetDirectionGeoOptimumBruteForce(
			@NotNull final PedestrianBHM pedestrianBHM,
			@NotNull final IPotentialFieldTarget targetPotentialField) {
		this.pedestrianBHM = pedestrianBHM;
		this.targetPotentialField = targetPotentialField;
		this.fallback = new TargetDirectionEuclidean(pedestrianBHM);
	}

	/**
	 * This method computes the target direction by a brute force optimization evaluating many equidistant points
	 * an the step circle of the this agent which is rather slow.
	 *
	 * @return the target direction
	 */
	@Override
	public VPoint getTargetDirection(@NotNull final Target target) {
		logger.warn("expensive optimization evaluation of " + nPoints);
		VPoint position = pedestrianBHM.getPosition();
		double stepLength = pedestrianBHM.getStepLength();

		VPoint gradient = targetPotentialField.getTargetPotentialGradient(position, pedestrianBHM).multiply(-1.0);
		double angle = GeometryUtils.angleTo(gradient, new VPoint(1, 0));
		List<VPoint> possibleNextPositions = GeometryUtils.getDiscDiscretizationPoints(
				new VCircle(position, stepLength),
				1,
				nPoints,
				angle,
				2*Math.PI);
		VPoint nextOptimalPos = possibleNextPositions.stream()
				//.filter(p -> !collidesWithObstacle(p))
				.min(Comparator.comparingDouble(p -> targetPotentialField.getPotential(p, pedestrianBHM))).get();

		// result is inside the target => take the shortest Euclidean step
		if(targetPotentialField.getPotential(nextOptimalPos, pedestrianBHM) <= 0) {
			return fallback.getTargetDirection(target);
		}
		else {
			return nextOptimalPos.subtract(position);
		}
	}
}
