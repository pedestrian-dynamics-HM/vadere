package org.vadere.simulator.models.osm.optimization;

import java.awt.Shape;
import java.util.List;
import java.util.Random;

import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.MultivariateRealOptimizer;
import org.apache.commons.math.optimization.direct.DirectSearchOptimizer;
import org.apache.commons.math.optimization.direct.NelderMead;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * The Class StepCircleOptimizerNelderMead.
 * 
 */
public class StepCircleOptimizerNelderMead implements StepCircleOptimizer {

	private static Logger logger = LogManager
			.getLogger(StepCircleOptimizerNelderMead.class);

	private final Random random;

	public StepCircleOptimizerNelderMead(Random random) {
		this.random = random;
	}

	@Override
	public VPoint getNextPosition(PedestrianOSM pedestrian, Shape reachableArea) {

		double stepSize = ((VCircle) reachableArea).getRadius();
		List<VPoint> positions = StepCircleOptimizerDiscrete.getReachablePositions(pedestrian, (VCircle)reachableArea, random);

		PotentialEvaluationFunction potentialEvaluationFunction = new PotentialEvaluationFunction(pedestrian);
		potentialEvaluationFunction.setStepSize(stepSize);

		double[] position = potentialEvaluationFunction.pointToArray(pedestrian.getPosition());
		double[] newPosition = new double[2];
		double[] minimum = position;
		double[] newMinimum = {0, 0};
		double minimumValue = pedestrian.getPotential(pedestrian.getPosition());
		double newMinimumValue = 0;
		double step = stepSize / 2;
		double threshold = 0.0001;

		MultivariateRealOptimizer optimizer = new NelderMead();

		try {

			double[][] simplex = new double[][] {{0, 0}, {step, step}, {step, -step}};
			((DirectSearchOptimizer) optimizer).setStartConfiguration(simplex);
			optimizer.setConvergenceChecker(new NelderMeadConvergenceChecker());
			newMinimum = optimizer.optimize(potentialEvaluationFunction, GoalType.MINIMIZE, minimum).getPoint();
			newMinimumValue = potentialEvaluationFunction.value(newMinimum);
			int counter = 0;

			if ((minimumValue > newMinimumValue && Math.abs(minimumValue - newMinimumValue) > threshold)) {
				minimumValue = newMinimumValue;
				minimum = newMinimum;
			}

			int bound = positions.size();

			while (counter < bound) {
				newPosition[0] = positions.get(counter).getX();
				newPosition[1] = positions.get(counter).getY();

				int anotherPoint;
				if (counter == bound - 1) {
					anotherPoint = 0;
				} else {
					anotherPoint = counter + 1;
				}

				double innerDistance = pedestrian.getPosition().distance(
						(positions.get(counter)));
				VPoint innerDirection = pedestrian.getPosition()
						.subtract((positions.get(counter)))
						.scalarMultiply(1.0 / innerDistance);
				double outerDistance = positions.get(anotherPoint).distance(
						(positions.get(counter)));
				VPoint outerDirection = positions.get(anotherPoint)
						.subtract((positions.get(counter)))
						.scalarMultiply(1.0 / outerDistance);

				simplex[1][0] = Math.min(step, innerDistance) * innerDirection.getX();
				simplex[1][1] = Math.min(step, innerDistance) * innerDirection.getY();
				simplex[2][0] = Math.min(step, outerDistance) * outerDirection.getX();
				simplex[2][1] = Math.min(step, outerDistance) * outerDirection.getY();

				((DirectSearchOptimizer) optimizer).setStartConfiguration(simplex);

				optimizer.setConvergenceChecker(new NelderMeadConvergenceChecker());
				newMinimum = optimizer.optimize(potentialEvaluationFunction,
						GoalType.MINIMIZE, newPosition).getPoint();
				newMinimumValue = potentialEvaluationFunction.value(newMinimum);

				counter++;

				if ((minimumValue > newMinimumValue && Math.abs(minimumValue - newMinimumValue) > threshold)) {
					minimumValue = newMinimumValue;
					minimum = newMinimum;
				}

			}

		} catch (ConvergenceException | FunctionEvaluationException e) {
			logger.error(e);
		}
		// System.out.println(potentialEvaluationFunction.counter);
		return new VPoint(minimum[0], minimum[1]);

	}

	public StepCircleOptimizer clone() {
		return new StepCircleOptimizerNelderMead(random);
	}
}
