package org.vadere.simulator.models.ode;

import java.util.List;

import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

public interface PedestrianGradientProvider {

	/**
	 * Computes the gradient, given time, the current pedestrian, his/her
	 * position x and velocity v.
	 * 
	 * @param t
	 *        current time
	 * @param currentPed
	 *        current pedestrian, if any
	 * @param x
	 *        position where the gradient should be evaluated
	 * @param v
	 *        velocity of the pedestrian
	 * @param grad
	 *        the result vector
	 */
	public void gradient(double t, Pedestrian currentPed, double[] x,
			double[] v, double[] grad);

	/**
	 * The setup function of the IGradientProvider. Is supposed to be called
	 * right before / in the same time step as all the calls to
	 * {@link #gradient(double, Pedestrian, double[], double[], double[])} are
	 * done.
	 * 
	 * @param pedestrianData
	 *        the current solution to the differential equation system
	 * @param Npersons
	 *        number of pedestrians in the current solution
	 */
	public void setup(double t, double[] pedestrianData, int Npersons);

	public List<Pedestrian> pedsInRange(VPoint pos, double radius);
}
