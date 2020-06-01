package org.vadere.simulator.models.osm.optimization;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.types.MovementType;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The Class StepCircleOptimizerDiscrete. Simple discrete optimizer, described
 * in [Seitz, 2012]
 * 
 */
public class StepCircleOptimizerDiscrete extends StepCircleOptimizer {

	private final double movementThreshold;  // the next position must improve at least by this amount
	private final Random random;
	private final static Logger log = Logger.getLogger(StepCircleOptimizerDiscrete.class);

	public StepCircleOptimizerDiscrete(final double movementThreshold, final Random random) {
		this.movementThreshold = movementThreshold;
		this.random = random;
	}

	@Override
	public VPoint getNextPosition(@NotNull final PedestrianOSM pedestrian, @NotNull final Shape reachableArea) {
		assert reachableArea instanceof VCircle;

		double stepSize = ((VCircle) reachableArea).getRadius();
		List<VPoint> positions = getReachablePositions(pedestrian, (VCircle) reachableArea, random);

		return getNextPosition(pedestrian, positions, stepSize);
	}

	public VPoint getNextPosition(@NotNull final PedestrianOSM pedestrian, final List<VPoint> positions,
								  final double stepSize){

		PotentialEvaluationFunction potentialEvaluationFunction = new PotentialEvaluationFunction(pedestrian);
		potentialEvaluationFunction.setStepSize(stepSize);

		VPoint curPos = pedestrian.getPosition();
		VPoint nextPos = curPos.clone();
		double curPosPotential = pedestrian.getPotential(curPos);
		double potential = curPosPotential;
		double currentPotential;

		for (VPoint currentPosition : positions) {
			try {
				currentPotential = potentialEvaluationFunction.getPotential(currentPosition);

				if(currentPotential < potential) {
					potential = currentPotential;
					nextPos = currentPosition;
				}
			} catch (Exception e) {
				Logger.getLogger(StepCircleOptimizerDiscrete.class).error("Potential evaluation threw an error: " + e.getMessage());
			}
		}

		// pedestrian.getTargetPotential(nextPos) > 0 => agent is not jet on his target otherwise the agent would wait forever
		if (curPosPotential - potential <= movementThreshold && pedestrian.getTargetPotential(nextPos) > 0) {
			nextPos = curPos;
		}

		return nextPos;
	}

	public SolutionPair computeBruteForceSolution(final PedestrianOSM pedestrian){
		// SolutionPair is defined in super class

		var reachableArea = new VCircle(pedestrian.getPosition(), pedestrian.getFreeFlowStepSize());
		var potentialEvaluationFunction = new PotentialEvaluationFunction(pedestrian);
		potentialEvaluationFunction.setStepSize(reachableArea.getRadius());

		VPoint optimalPoint = getNextPosition(pedestrian, getBruteForcePointsCircles(reachableArea),
				reachableArea.getRadius());

		double optimalFuncValue;  // -1 = invalid number
		try{
			optimalFuncValue = potentialEvaluationFunction.getValue(optimalPoint);
		}catch (Exception e) {
			Logger.getLogger(StepCircleOptimizerDiscrete.class).error("Potential evaluation for computing the brute " +
					"force solution threw error. Setting value to invalid (-1).");
			optimalFuncValue = -1;
		}

		return new SolutionPair(optimalPoint, optimalFuncValue);
	}

	public StepCircleOptimizer clone() {
		return new StepCircleOptimizerDiscrete(movementThreshold, random);
	}

	public static List<VPoint> getReachablePositions(@NotNull final PedestrianOSM pedestrian,
													 @NotNull VCircle reachableArea, @NotNull final Random random) {

		final AttributesOSM attributesOSM = pedestrian.getAttributesOSM();
		int numberOfCircles = attributesOSM.getNumberOfCircles();
		// if number of circle is negative, choose number of circles according to
		// StepCircleResolution
		if (attributesOSM.getNumberOfCircles() < 0) {
		    throw new IllegalArgumentException("number of circles is negative ("+attributesOSM.getNumberOfCircles()+")");

			/*
			 * Intention of this code snippet is unclear, therefore not jet removed.‚
			numberOfCircles = (int) Math.ceil(attributesOSM
					.getStepCircleResolution() / (2 * Math.PI));*/
		}

		// maximum possible angle3D of movement relative to ankerAngle
		double angle;

		// smallest possible angle3D of movement
		double anchorAngle;

		// compute maximum angle3D and corresponding anchor if appropriate
		if (attributesOSM.getMovementType() == MovementType.DIRECTIONAL) {
		    //TODO: this code snippet has to be understood and maybe reformulate / explained
            /*
             * velocity dependent choice of the walking direction i.e. if pedestrians move fast they can not
             * change their direction much.
             */
            log.warn("use of unexplained code!");
			angle = getMovementAngle(pedestrian);
			Vector2D velocity = pedestrian.getVelocity();
			anchorAngle = velocity.angleToZero() - angle;
			angle = 2 * angle;
		} else {
			angle = 2 * Math.PI;
			anchorAngle = 0;
		}

		return GeometryUtils.getDiscDiscretizationPoints(
				random,
				attributesOSM.isVaryStepDirection(),
				reachableArea,
				numberOfCircles,
				attributesOSM.getStepCircleResolution(),
				anchorAngle,
				angle);

	}

	private static List<VPoint> getBruteForcePointsCircles(VCircle reachableArea){
		// NOTE: numberPointsOfLargestCircle and numberOfCircles are parameters with a trade off between runtime and
		// precision of brute force solution

		return GeometryUtils.getDiscDiscretizationPoints(
				null,
				false,
				reachableArea,
				100,
				2000,
				0,
				2.0 * Math.PI);
	}

	private static List<VPoint> getBruteForcePointsLines(VCircle reachableArea) {
		/* Just an alternative to the getBruteForcePointsCircles (which is recommended to use).*/

		// both have to be larger than 2
		final int nrLines = 2000;
		final int nrPointsPerLine = 2000;

		VPoint centerPoint = reachableArea.getCenter();

		final double intervalXDirection = 2 * reachableArea.getRadius() / (nrPointsPerLine - 1);
		final double intervalYDirection = 2 * reachableArea.getRadius() / (nrLines - 1);

		VPoint refPoint = new VPoint(centerPoint.x - reachableArea.getRadius(), centerPoint.y + reachableArea.getRadius());

		List<VPoint> returnList = new ArrayList<>();

		for (int i = 0; i < nrLines; ++i) {
			for (int j = 0; j < nrPointsPerLine; ++j) {
				VPoint currentPoint = new VPoint(refPoint.x + intervalXDirection * j, refPoint.y);
				if(currentPoint.distance(centerPoint) <= reachableArea.getRadius()){
					returnList.add(currentPoint.clone());
				}
			}
			refPoint.y -= intervalYDirection;
		}

		return returnList;
	}

	/**
	 * The maximum deviation from the last movement direction given the current speed.
     * See seitz-2016 PhD-thesis equation 4.6
	 */
	public static double getMovementAngle(@NotNull final PedestrianOSM pedestrian) {

		final double speed = pedestrian.getVelocity().getLength();
		double result = Math.PI - speed;

		if (result < 0.1) {
			result = 0.1;
		}
		return result;
	}

}
