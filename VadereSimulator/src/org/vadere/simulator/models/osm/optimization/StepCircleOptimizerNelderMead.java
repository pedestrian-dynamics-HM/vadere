package org.vadere.simulator.models.osm.optimization;

import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.direct.NelderMead;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The class StepCircleOptimizerNelderMead.
 * 
 */
public class StepCircleOptimizerNelderMead extends StepCircleOptimizer {

	private static Logger logger = Logger
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

		NelderMead optimizer = new NelderMead();

		try {

			/*if(lastSolution.containsKey(pedestrian)) {
				VPoint optimum = lastSolution.get(pedestrian).add(pedestrian.getPosition());
				if(isLocalMinimum(potentialEvaluationFunction, (VCircle) reachableArea, optimum)) {
					logger.info("quick solution found.");
					return optimum;
				}
			}*/

			//minimum = position;
			double[][] simplex = new double[][] {{0, 0}, {step, step}, {step, -step}};
			optimizer.setStartConfiguration(simplex);
			optimizer.setConvergenceChecker(new NelderMeadConvergenceChecker());
			newMinimum = optimizer.optimize(potentialEvaluationFunction, GoalType.MINIMIZE, position).getPoint();
			//logger.info("["+0+","+0+"],["+step+","+step+"],["+step+","+(-step)+")]");
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

				//logger.info("["+simplex[0][0]+","+simplex[0][1]+"],["+simplex[1][0]+","+simplex[1][1]+"],["+simplex[2][0]+","+simplex[2][1]+")]");

				optimizer.setStartConfiguration(simplex);

				optimizer.setConvergenceChecker(new NelderMeadConvergenceChecker());
				newMinimum = optimizer.optimize(potentialEvaluationFunction,
						GoalType.MINIMIZE, newPosition).getPoint();
				newMinimumValue = potentialEvaluationFunction.value(newMinimum);

				counter++;

				if ((minimumValue > newMinimumValue && Math.abs(minimumValue - newMinimumValue) > threshold)) {
					minimumValue = newMinimumValue;
					minimum = newMinimum;
					//logger.info("new min: ["+minimum[0]+","+minimum[1]+"]");
				}

			}

		} catch (ConvergenceException | FunctionEvaluationException e) {
			logger.error(e);
		}
		// System.out.println(potentialEvaluationFunction.counter);
		//logger.info("["+(minimum[0]-pedestrian.getPosition().getX())+","+(minimum[1]-pedestrian.getPosition().getY())+"]");
		//lastSolution.put(pedestrian, new VPoint(minimum[0]-pedestrian.getPosition().getX(), minimum[1]-pedestrian.getPosition().getY()));

		if(getIsComputeMetric()){
			// See merge request !65
			this.computeAndAddBruteForceSolutionMetric(pedestrian,
                    new SolutionPair(new VPoint(minimum[0], minimum[1]), minimumValue));
		}

		return new VPoint(minimum[0], minimum[1]);

	}

	public StepCircleOptimizer clone() {
		return new StepCircleOptimizerNelderMead(random);
	}

	private boolean isLocalMinimum(PotentialEvaluationFunction evaluationFunction, VCircle stepDisc, VPoint optimum) throws FunctionEvaluationException {
		double delta = 0.0001;
		double angle = 0.05 * 2 * Math.PI;
		double value = evaluationFunction.getValue(optimum);

		for(double angleDelta = 0; angleDelta <= 2 * Math.PI; angleDelta += angle) {
			VPoint newPoint = optimum.add(new VPoint(delta, 0).rotate(angleDelta));
			if(stepDisc.contains(newPoint) && evaluationFunction.getValue(newPoint) < value) {
				return false;
			}
		}
		return evaluationFunction.getValue(stepDisc.getCenter()) > value;
	}
}
