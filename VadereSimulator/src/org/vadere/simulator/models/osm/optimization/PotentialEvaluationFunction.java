package org.vadere.simulator.models.osm.optimization;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.MultivariateRealFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.List;

/**
 * The Class PotentialEvaluationFunction.
 * 
 */
public class PotentialEvaluationFunction implements UnivariateRealFunction,
		MultivariateRealFunction, MultivariateFunction {

	/** The pedestrian. */
	private final PedestrianOSM pedestrian;

	/** The step size. */
	private double stepSize;
	private double minStepSize;

	public int counter;

	/**
	 * Instantiates a new potential evaluation function.
	 * 
	 * @param pedestrian
	 *        the considered pedestrian
	 */
	PotentialEvaluationFunction(final PedestrianOSM pedestrian) {
		this.pedestrian = pedestrian;
		this.minStepSize = pedestrian.getMinStepLength();
		this.stepSize = 0;
		this.counter = 0;
	}

	/**
	 * Sets the step size for the pedestrian.
	 * 
	 * @param stepSize
	 *        the new step size
	 */
	public void setStepSize(double stepSize) {
		this.stepSize = stepSize;
	}

	/**
	 * Returns the considered pedestrian.
	 */
	public Pedestrian getPedestrian() {
		return pedestrian;
	}

	/**
	 * Returns the value of the aggregated potential. Required method for
	 * optimization by Brent.
	 * 
	 * @param angle
	 *        the angle to the relevant position
	 * @return the potential value
	 * @throws FunctionEvaluationException
	 *         the function evaluation exception
	 */
	@Override
	public double value(double angle) throws FunctionEvaluationException {
		VPoint pedPos = pedestrian.getPosition();
		VPoint newPos = new VPoint(stepSize * Math.cos(angle) + pedPos.x,
				stepSize * Math.sin(angle) + pedPos.y);
		return pedestrian.getPotential(newPos);
	}

	/**
	 * Returns the target potential.
	 * 
	 * @param angle
	 *        the angle of the direction to new position
	 */
	public double getTargetPotential(double angle) {
		VPoint pedPos = pedestrian.getPosition();
		VPoint newPos = new VPoint(stepSize * Math.cos(angle) + pedPos.x,
				stepSize * Math.sin(angle) + pedPos.y);

		return pedestrian.getTargetPotential(newPos);
	}

	/**
	 * Returns the value of the aggregated potential. Auxiliary method for
	 * optimization.
	 * 
	 * @param pos
	 *        the relevant position
	 * @return the potential value
	 * @throws FunctionEvaluationException
	 *         the function evaluation exception
	 * @throws IllegalArgumentException
	 *         the illegal argument exception
	 */
	public double getValue(VPoint pos) throws FunctionEvaluationException,
			IllegalArgumentException {
		double result = value(this.pointToArray(pos));
		return result;
	}

	/**
	 * Returns the value of the aggregated potential. Required method for
	 * optimization by NelderMead.
	 * 
	 * @param pos
	 *        the relevant position
	 * @return the potential value
	 * @throws FunctionEvaluationException
	 *         the function evaluation exception
	 * @throws IllegalArgumentException
	 *         the illegal argument exception
	 */
	@Override
	public double value(double[] pos) {
		VPoint pedPos = pedestrian.getPosition();
		VPoint newPos = new VPoint(pos[0], pos[1]);
		double result = 100000;
		// TODO: this is a dirty hack, fix it!
		if (pedestrian.getAttributesOSM().isSeeSmallWalls()) {
			List<Obstacle> obstacles = pedestrian.getTopography().getObstacles();
			for (Obstacle obstacle : obstacles) {
				if (obstacle.getShape().intersects(new VLine(pedPos, newPos)))
					return result;
			}
		}

		if (Math.pow(newPos.x - pedPos.x, 2) + Math.pow(newPos.y - pedPos.y, 2) <= Math.pow(stepSize, 2) + 0.00001) {
			result = pedestrian.getPotential(newPos);
		}
		counter++;
		return result;
	}

	/**
	 * Returns the value of the aggregated potential. Auxiliary method for
	 * optimization.
	 * 
	 * @param position
	 *        the relevant position
	 * @return the potential value
	 * @throws FunctionEvaluationException
	 *         the function evaluation exception
	 * @throws IllegalArgumentException
	 *         the illegal argument exception
	 */
	public double getPotential(VPoint position)
			throws FunctionEvaluationException, IllegalArgumentException {
		return value(this.pointToArray(position));
	}

	/**
	 * Converts a point - position '(x,y)' - into an array.
	 * 
	 * @param point
	 * @return the array with x = array[0] and y = array[1]
	 */
	public double[] pointToArray(VPoint point) {
		double[] array = new double[2];
		array[0] = point.x;
		array[1] = point.y;
		return array;
	}

}
