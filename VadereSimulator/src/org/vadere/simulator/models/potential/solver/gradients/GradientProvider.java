package org.vadere.simulator.models.potential.solver.gradients;

/**
 * Defines the interface of a general gradient provider. An IGradientProvider is
 * supposed to provide a gradient at a given time and position and can be
 * provided with several additional data like the pedestrian at the position, or
 * the current speed.
 * 
 */
public interface GradientProvider {
	/**
	 * Computes the gradient, given time, the current pedestrian, his/her
	 * position x and velocity v.
	 * 
	 * @param t
	 *        current time
	 * @param currentTargetId
	 *        id of the target of the current pedestrian
	 * @param x
	 *        position where the gradient should be evaluated
	 * @param v
	 *        velocity of the pedestrian
	 * @param grad
	 *        the result vector
	 */
	void gradient(double t, int currentTargetId, double[] x, double[] grad);
}
