package org.vadere.simulator.models.osm.optimization;

import org.apache.log4j.Logger;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.types.MovementType;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;

import java.awt.*;
import java.util.LinkedList;
import java.util.Random;

/**
 * The Class StepCircleOptimizerDiscrete. Simple discrete optimizer, described
 * in [Seitz, 2012]
 * 
 */
public class StepCircleOptimizerDiscrete implements StepCircleOptimizer {

	private final double movementThreshold;
	private final Random random;

	public StepCircleOptimizerDiscrete(final double movementThreshold, Random random) {
		this.movementThreshold = movementThreshold;
		this.random = random;
	}

	@Override
	public VPoint getNextPosition(PedestrianOSM pedestrian, Shape reachableArea) {

		double stepSize = ((VCircle) reachableArea).getRadius();
		LinkedList<VPoint> positions = getReachablePositions(pedestrian, random);

		PotentialEvaluationFunction potentialEvaluationFunction = new PotentialEvaluationFunction(
				pedestrian);
		potentialEvaluationFunction.setStepSize(stepSize);

		VPoint curPos = pedestrian.getPosition();
		VPoint nextPos = curPos.clone();
		double curPosPotential = pedestrian.getPotential(curPos);
		double potential = curPosPotential;
		double tmpPotential = 0;



		for (VPoint tmpPos : positions) {
			try {
				tmpPotential = potentialEvaluationFunction.getValue(tmpPos);

				if (tmpPotential < potential
						|| (Math.abs(tmpPotential - potential) <= 0.0001 && random
								.nextBoolean())) {
					potential = tmpPotential;
					nextPos = tmpPos.clone();
				}
			} catch (Exception e) {
				Logger.getLogger(StepCircleOptimizerDiscrete.class).error("Potential evaluation threw an error.");
			}

		}

		if (curPosPotential - potential < movementThreshold) {
			nextPos = curPos;
			potential = curPosPotential;
		}
		return nextPos;
	}

	public StepCircleOptimizer clone() {
		return new StepCircleOptimizerDiscrete(movementThreshold, random);
	}

	public static LinkedList<VPoint> getReachablePositions(final PedestrianOSM pedestrian, final Random random) {

		final AttributesOSM attributesOSM = pedestrian.getAttributesOSM();
		double randOffset = attributesOSM.isVaryStepDirection() ? random.nextDouble() : 0;

		VPoint currentPosition = pedestrian.getPosition();
		LinkedList<VPoint> reachablePositions = new LinkedList<VPoint>();
		int numberOfCircles = attributesOSM.getNumberOfCircles();
		double circleOfGrid = 0;
		int numberOfGridPoints;

		// if number of circle is negative, choose number of circles according to
		// StepCircleResolution
		if (attributesOSM.getNumberOfCircles() < 0) {
			numberOfCircles = (int) Math.ceil(attributesOSM
					.getStepCircleResolution() / (2 * Math.PI));
		}

		// maximum possible angle of movement relative to ankerAngle
		double angle;

		// smallest possible angle of movement
		double anchorAngle;

		// compute maximum angle and corresponding anchor if appropriate
		if (attributesOSM.getMovementType() == MovementType.DIRECTIONAL) {
			angle = getMovementAngle(pedestrian);
			Vector2D velocity = pedestrian.getVelocity();
			anchorAngle = velocity.angleToZero() - angle;
			angle = 2 * angle;
		} else {
			angle = 2 * Math.PI;
			anchorAngle = 0;
		}

		// iterate through all circles
		for (int j = 1; j <= numberOfCircles; j++) {

			circleOfGrid = pedestrian.getStepSize() * j / numberOfCircles;

			numberOfGridPoints = (int) Math.ceil(circleOfGrid / pedestrian.getStepSize()
					* attributesOSM.getStepCircleResolution());

			// reduce number of grid points proportional to the constraint of direction
			if (attributesOSM.getMovementType() == MovementType.DIRECTIONAL) {
				numberOfGridPoints = (int) Math.ceil(numberOfGridPoints * angle / (2 * Math.PI));
			}

			double angleDelta = angle / numberOfGridPoints;

			// iterate through all angles and compute absolute positions of grid points
			for (int i = 0; i < numberOfGridPoints; i++) {

				double x = circleOfGrid * Math.cos(anchorAngle + angleDelta * (randOffset + i)) + currentPosition.x;
				double y = circleOfGrid * Math.sin(anchorAngle + angleDelta * (randOffset + i)) + currentPosition.y;
				VPoint tmpPos = new VPoint(x, y);

				reachablePositions.add(tmpPos);
			}
		}
		return reachablePositions;
	}

	/**
	 * The maximum deviation from the last movement direction given the current speed.
	 */
	private static double getMovementAngle(PedestrianOSM pedestrian) {

		final double speed = pedestrian.getVelocity().getLength();
		double result = Math.PI - speed;

		if (result < 0.1) {
			result = 0.1;
		}
		return result;
	}

}
