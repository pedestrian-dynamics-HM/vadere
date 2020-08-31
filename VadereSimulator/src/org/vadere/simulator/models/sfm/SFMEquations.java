package org.vadere.simulator.models.sfm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.vadere.simulator.models.ode.AbstractModelEquations;
import org.vadere.simulator.models.ode.ODEModel;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.MathUtil;
import org.vadere.util.parallel.AParallelWorker;
import org.vadere.util.parallel.AParallelWorker.Work;
import org.vadere.util.parallel.CountableParallelWorker;
import org.vadere.util.parallel.IAsyncComputable;

/**
 * Implementation of the Social Force Model (Helbing 1995). This implementation
 * only captures very basic functionality:<br>
 * 
 * <pre>
 * dx/dt = v * normalizer(v)
 * dv/dt = (-grad_floorfield * vmax - v) * accTarget - grad_obstacles - grad_pedestrians
 * </pre>
 * 
 * The three different gradients are provided through instances of
 * IGradientProvider given in the constructor.
 * 
 */
public class SFMEquations extends AbstractModelEquations<Pedestrian> implements IAsyncComputable {

	private static Logger logger = Logger.getLogger(SFMEquations.class);


	/**
	 * Four dimensions: 2 for position, 2 for velocity
	 */
	@Override
	protected int dimensionPerPerson() {
		return 4;
	}

	/**
	 * Implement the right hand side of the Social Force Model equations.
	 * 
	 * @see org.apache.commons.math3.ode.FirstOrderDifferentialEquations#computeDerivatives(double,
	 *      double[], double[])
	 */
	@Override
	public void computeDerivatives(final double t, double[] y, final double[] yDot) {


		// update the pedestrian positions in the topography to the ones computed in the integrator
		ODEModel.updateElementPositions(Pedestrian.class, t, topography, this, y);

		// create workers for each pedestrian
		List<AParallelWorker<Double[]>> workers = new ArrayList<AParallelWorker<Double[]>>();

		// loop over all persons and compute the next step
		int personCounter = 0; // used for arrays, not identical to personID!
		for (final Pedestrian pedestrian : elements) {
			AParallelWorker<Double[]> w = new CountableParallelWorker<Double[]>(
					personCounter, new Work<Double[]>() {
						private int ID;

						@Override
						public Double[] call() throws Exception {
							computeSinglePerson(pedestrian, getWorkerID(), t,
									y, yDot);
							return null;
						}

						@Override
						public void setID(int ID) {
							this.ID = ID;
						}

						@Override
						public int getWorkerID() {
							return this.ID;
						}
					});
			personCounter++;
			workers.add(w);
			w.start();
		}

		// finish the work
		for (AParallelWorker<Double[]> worker : workers) {
			try {
				worker.finish();
			} catch (ExecutionException e) {
				logger.error(e);
				e.printStackTrace();
			} catch (InterruptedException e) {
				// Necessary in order to tell Simulation the thread has been
				// interrupted.
				// TODO [priority=low] [task=refactoring] Hack, other solution?
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	/**
	 * Computes yDot for a single person given by personID. This is computed
	 * asynchronously by an {@link AParallelWorker}.
	 *
	 * @param currentPed
	 * @param personCounter
	 * @param t
	 * @param y
	 * @param yDot
	 */
	private void computeSinglePerson(Pedestrian currentPed, int personCounter, double t, double[] y, double[] yDot) {
		double[] position = new double[2];
		double[] velocity = new double[2];
		double[] positionDot = new double[2];
		double[] velocityDot = new double[2];
		double[] grad_field = new double[2];
		double[] viewing_direction = new double[2];
		// ///////////////////////////////////////
		// extract data

		// position
		getPosition(personCounter, y, position);
		// velocity
		getVelocity(personCounter, y, velocity);

		// ///////////////////////////////////////
		// generate gradients
		assert (!Double.isNaN(position[0]));
		assert (!Double.isNaN(position[1]));

		// get the static gradient
		staticGradientProvider.gradient(t, currentPed.getNextTargetId(),
				position, grad_field);
		assert (!Double.isNaN(position[0]));
		assert (!Double.isNaN(position[1]));

		/*
		 * Target target = this.topography.getTarget(currentPed.getTargets()
		 * .get(0));
		 * double distance2Target = target.getShape().distance(
		 * new VPoint(position[0], position[1]));
		 * if (distance2Target < 1) {
		 * MathUtil.normalize(grad_field);
		 * MathUtil.mult(grad_field, distance2Target);
		 * }
		 */

		// compute the velocity from the static gradient.
		viewing_direction[0] = -grad_field[0];
		viewing_direction[1] = -grad_field[1];

		double normViewingDir = MathUtil.norm2(viewing_direction);
		// set to the correct length (= speed)
		if (normViewingDir > 0) {
			MathUtil.normalize(viewing_direction);
		}

		VPoint pos = new VPoint(position[0], position[1]);
		Vector2D vel = new Vector2D(velocity[0], velocity[1]);

		// get the static gradient for obstacles
		Vector2D obstacleGradient = obstacleGradientProvider
				.getObstaclePotentialGradient(pos, currentPed);
		// get the dynamic gradient for pedestrians
		Collection<? extends Agent> otherPedestrians = pedestrianGradientProvider
				.getRelevantAgents(new VCircle(pos, 0.1), currentPed,
						topography);
		Vector2D pedestrianGradient = pedestrianGradientProvider
				.getAgentPotentialGradient(pos, vel, currentPed,
						otherPedestrians);

		// get ped speed and acceleration data
		double v0 = currentPed.getFreeFlowSpeed();
		double acc = currentPed.getAcceleration();

		// perform the computational steps that define this equation model
		positionDot[0] = velocity[0]
				* normalizer(v0 * 1.3, MathUtil.norm2(velocity));
		positionDot[1] = velocity[1]
				* normalizer(v0 * 1.3, MathUtil.norm2(velocity));

		velocityDot[0] = (-grad_field[0] * v0 - velocity[0]) * acc
				- obstacleGradient.x - pedestrianGradient.x;
		velocityDot[1] = (-grad_field[1] * v0 - velocity[1]) * acc
				- obstacleGradient.y - pedestrianGradient.y;

		assert (!Double.isNaN(positionDot[0]));
		assert (!Double.isNaN(positionDot[1]));
		assert (!Double.isNaN(velocityDot[0]));
		assert (!Double.isNaN(velocityDot[1]));

		// store data
		setPosition(personCounter, yDot, positionDot);
		setVelocity(personCounter, yDot, velocityDot);
	}

	/**
	 * Normalizes the given value if it is greater than vmax.
	 * 
	 * @param vmax
	 *        maximum value
	 * @param normv
	 *        value that will be normalized
	 * @return (normv <= vmax) ? (vmax/normv) : 1;
	 */
	private double normalizer(double vmax, double normv) {
		if (normv <= vmax) {
			return 1;
		} else {
			return vmax / normv;
		}
	}

	@Override
	public void setVelocity(int personID, double[] solution, double[] velocity) {
		// only the first component is used in the GNM
		solution[personID * dimensionPerPerson() + 2] = velocity[0];
		solution[personID * dimensionPerPerson() + 3] = velocity[1];
	}

	@Override
	public void getVelocity(int personID, double[] solution, double[] velocity) {
		velocity[0] = solution[personID * dimensionPerPerson() + 2];
		velocity[1] = solution[personID * dimensionPerPerson() + 3];
	}
}
