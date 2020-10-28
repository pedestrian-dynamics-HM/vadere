package org.vadere.simulator.models.bhm.helpers.targetdirection;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.bhm.PedestrianBHM;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.state.scenario.Target;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.GoldenSectionSearch;
import java.util.function.Function;

public class TargetDirectionGeoOptimum implements TargetDirection {
	private static final Logger logger = Logger.getLogger(TargetDirectionGeoOptimum.class);
	private PedestrianBHM pedestrianBHM;
	private IPotentialFieldTarget targetPotentialField;
	private TargetDirection fallBackStrategy;

	public TargetDirectionGeoOptimum(
			@NotNull final PedestrianBHM pedestrianBHM,
			@NotNull final IPotentialFieldTarget targetPotentialField) {
		this.pedestrianBHM = pedestrianBHM;
		this.targetPotentialField = targetPotentialField;
		this.fallBackStrategy = new TargetDirectionGeoOptimumBruteForce(pedestrianBHM, targetPotentialField);
	}

	/**
	 * This method computes the target direction by a golden section search of the target potential function
	 * on the step circle of this agent.
	 *
	 * @return the target direction
	 */
	@Override
	public VPoint getTargetDirection(@NotNull final Target target) {
		VPoint position = pedestrianBHM.getPosition();
		VPoint lastPosition = pedestrianBHM.getLastPosition();
		double stepLength = pedestrianBHM.getStepLength();

		logger.info("optimum direction");
		VPoint gradient = targetPotentialField.getTargetPotentialGradient(position, pedestrianBHM).multiply(-1.0);
		double tol = 0.01; //
		double a0 = -0.99 * Math.PI;
		double b0 = 0.99 * Math.PI;
		double gradLen = gradient.distanceToOrigin();

		if(gradLen < GeometryUtils.DOUBLE_EPS && lastPosition != null) {
			gradient = position.subtract(lastPosition);
		} else if(gradLen < GeometryUtils.DOUBLE_EPS) {
			logger.warn("no valid gradient!");
			gradient = new VPoint(1,0);
		}

		VPoint bestArg = gradient.setMagnitude(stepLength);
		Function<Double, Double> f = rad -> targetPotentialField.getPotential(bestArg.rotate(rad).add(position), pedestrianBHM);
		double[] ab = GoldenSectionSearch.gss(f, a0, b0, tol);
		double rad = ab[1];
		double fa = f.apply(ab[0]);
		double fb = f.apply(ab[1]);
		double bestVal = fb;

		if(fa < f.apply(ab[1])) {
			rad = ab[0];
			bestVal = fa;
		}

		double currentPotential = targetPotentialField.getPotential(position, pedestrianBHM);
		double firstGuess = f.apply(0.0);
		// test if the first guess i.e. the point in (negative) gradient direction is even better.
		if(firstGuess < bestVal && firstGuess < currentPotential) {
			return bestArg.norm();
		} else if(bestVal < currentPotential) {
			return bestArg.rotate(rad).norm();
		} else {
			// in this case this optimization failed i.e. the agent would go backwards! => back to brute force!
			return fallBackStrategy.getTargetDirection(target);
		}
	}
}
