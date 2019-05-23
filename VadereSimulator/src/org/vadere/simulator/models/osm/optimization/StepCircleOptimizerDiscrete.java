package org.vadere.simulator.models.osm.optimization;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.types.MovementType;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.logging.Logger;

import java.awt.*;
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

	public StepCircleOptimizerDiscrete(final double movementThreshold, @NotNull final Random random) {
		this.movementThreshold = movementThreshold;
		this.random = random;
	}

	@Override
	public VPoint getNextPosition(@NotNull final PedestrianOSM pedestrian, @NotNull final Shape reachableArea) {
		assert reachableArea instanceof VCircle;

		double stepSize = ((VCircle) reachableArea).getRadius();
		List<VPoint> positions = getReachablePositions(pedestrian, (VCircle) reachableArea, random);

		return getNextPosition(pedestrian, positions, stepSize, false);
	}

	public VPoint getNextPosition(@NotNull final PedestrianOSM pedestrian, final List<VPoint> positions,
								  final double stepSize, final boolean bruteForce){

		/*bruteForce gets exactly the best (numerical) position among the tested 'positions' without any side
		* conditions.
		* */

		PotentialEvaluationFunction potentialEvaluationFunction = new PotentialEvaluationFunction(pedestrian);
		potentialEvaluationFunction.setStepSize(stepSize);

		VPoint curPos = pedestrian.getPosition();
		VPoint nextPos = curPos.clone();
		double curPosPotential = pedestrian.getPotential(curPos);
		double potential = curPosPotential;
		double currentPotential = 0;

		for (VPoint currentPosition : positions) {
			try {
				currentPotential = potentialEvaluationFunction.getValue(currentPosition);

				// DL: it is not exactly clear how this condition works (where is the value 0.0001 coming from?, Why
				// is there a random boolean?
				boolean fineTuneCondition = (Math.abs(currentPotential - potential) <= 0.0001 && random.nextBoolean());

				if (currentPotential < potential || (bruteForce || fineTuneCondition)) {
					potential = currentPotential;
					nextPos = currentPosition;
				}
			} catch (Exception e) {
				Logger.getLogger(StepCircleOptimizerDiscrete.class).error("Potential evaluation threw an topographyError.");
			}

		}

		if (bruteForce || curPosPotential - potential <= movementThreshold) {
			nextPos = curPos;
		}

		return nextPos;
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

		// maximum possible angle of movement relative to ankerAngle
		double angle;

		// smallest possible angle of movement
		double anchorAngle;

		// compute maximum angle and corresponding anchor if appropriate
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
