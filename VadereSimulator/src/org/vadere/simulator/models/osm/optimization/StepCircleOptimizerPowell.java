package org.vadere.simulator.models.osm.optimization;

import java.awt.Shape;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.optimization.direct.BOBYQAOptimizer;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.analysis.MultivariateFunction;

public class StepCircleOptimizerPowell extends StepCircleOptimizer {


	private Random random;

	public StepCircleOptimizerPowell(Random random) {
		this.random = random;
	}

	@Override
	public VPoint getNextPosition(PedestrianOSM pedestrian, Shape reachableArea) {

		double stepSize = ((VCircle) reachableArea).getRadius();

		PotentialEvaluationFunction potentialEvaluationFunction = new PotentialEvaluationFunction(pedestrian);
		potentialEvaluationFunction.setStepSize(stepSize / 2);

		double[] position = potentialEvaluationFunction.pointToArray(pedestrian.getPosition());
		double[] newPosition = position;

		BOBYQAOptimizer optimizer = new BOBYQAOptimizer(4, 0.5, 0.0001);

		PointValuePair minimum = new PointValuePair(position, potentialEvaluationFunction.value(position));

		PointValuePair newMinimum = new PointValuePair(position, potentialEvaluationFunction.value(position));

		List<VPoint> positions = StepCircleOptimizerDiscrete.getReachablePositions(pedestrian, (VCircle)reachableArea, random);

		try {
			newMinimum = optimizer.optimize(1000, (MultivariateFunction) potentialEvaluationFunction, GoalType.MINIMIZE,
					position);
		} catch (Exception e) {

		}
		if (newMinimum.getValue() <= minimum.getValue()) {
			minimum = new PointValuePair(newMinimum.getPoint(), newMinimum.getValue());
		}

		for (int i = 0; i < positions.size(); i++) {

			newPosition[0] = positions.get(i).getX();
			newPosition[1] = positions.get(i).getY();
			try {
				newMinimum = optimizer.optimize(1000, (MultivariateFunction) potentialEvaluationFunction,
						GoalType.MINIMIZE, newPosition);
			} catch (Exception e) {

			}
			if (newMinimum.getValue() <= minimum.getValue()) {
				minimum = new PointValuePair(newMinimum.getPoint(), newMinimum.getValue());
			}

		}

		return new VPoint(minimum.getPoint()[0], minimum.getPoint()[1]);
	}

	public StepCircleOptimizer clone() {
		return new StepCircleOptimizerPowell(random);
	}
}
