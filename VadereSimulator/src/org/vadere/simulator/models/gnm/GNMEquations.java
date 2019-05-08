package org.vadere.simulator.models.gnm;

import org.vadere.simulator.models.ode.AbstractModelEquations;
import org.vadere.simulator.models.ode.ODEModel;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.logging.Logger;
import org.vadere.util.math.MathUtil;
import org.vadere.util.parallel.AParallelWorker;
import org.vadere.util.parallel.AParallelWorker.Work;
import org.vadere.util.parallel.CountableParallelWorker;
import org.vadere.util.parallel.IAsyncComputable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * The equations of the Gradient Navigation Model.
 * 
 */
public class GNMEquations extends AbstractModelEquations<Pedestrian> implements
		IAsyncComputable {

	private static Logger logger = Logger.getLogger(GNMEquations.class);

	/**
	 * Three dimensions: 2 for dx/dt, 1 for dv/dt
	 */
	@Override
	protected int dimensionPerPerson() {
		return 3;
	}

	@Override
	public void computeDerivatives(final double t, final double[] y,
			final double[] yDot) {

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
	private void computeSinglePerson(Pedestrian currentPed, int personCounter,
			double t, double[] y, double[] yDot) {
		double[] position = new double[2];
		double[] speed = new double[2];
		double[] grad_field = new double[2];
		double[] viewing_direction = new double[2];

		// ///////////////////////////////////////
		// extract data

		// position
		getPosition(personCounter, y, position);
		// speed
		getVelocity(personCounter, y, speed);

		// ///////////////////////////////////////
		// generate gradients

		VCircle relevantArea = new VCircle(
				new VPoint(position[0], position[1]), 0.01);
		Collection<? extends Agent> otherPeds = pedestrianGradientProvider
				.getRelevantAgents(relevantArea, currentPed, topography);
		otherPeds.remove(currentPed);

		// get the static gradient
		if (currentPed.hasNextTarget()) {
			staticGradientProvider.gradient(t, currentPed.getNextTargetId(),
					position, grad_field);
		}

		// compute the velocity from the static gradient.
		viewing_direction[0] = -grad_field[0];
		viewing_direction[1] = -grad_field[1];

		double normViewingDir = MathUtil.norm2(viewing_direction);
		// set to the correct length (= speed)
		if (normViewingDir > 0) {
			MathUtil.normalize(viewing_direction);
			// MathUtil.mult(viewing_direction, speed[0]);
		} else {
			// viewing direction is a combination of all other peds in the
			// vincinity
			for (Agent p : otherPeds) {
				viewing_direction[0] += p.getPosition().x - position[0];
				viewing_direction[1] += p.getPosition().y - position[1];
			}
			MathUtil.normalize(viewing_direction);
		}
		currentPed.setVelocity(new Vector2D(viewing_direction[0],
				viewing_direction[1]));

		// get the gradient for obstacles
		Vector2D grad_obstacles = obstacleGradientProvider
				.getObstaclePotentialGradient(new VPoint(position[0],
						position[1]), currentPed);

		// get the gradient for pedestrians
		Vector2D grad_pedestrians = pedestrianGradientProvider
				.getAgentPotentialGradient(new VPoint(position[0],
						position[1]), new Vector2D(viewing_direction[0],
								viewing_direction[1]),
						currentPed, otherPeds);

		// combine gradients
		Vector2D totalDynamicGradient = new Vector2D(0, 0)
				.add(grad_pedestrians).add(grad_obstacles);
		Vector2D totalStaticGradient = new Vector2D(grad_field[0],
				grad_field[1]);

		Vector2D totalGradient = g(g(totalStaticGradient).add(
				g(totalDynamicGradient)));

		// get ped speed and acceleration data
		double vdes = currentPed.getFreeFlowSpeed();
		double acc = currentPed.getAcceleration();

		// compute density around the ped, adjust speed according to Weidmann
		// int pedsInRange = getVisiblePedestrians(otherPeds2).size() + 1;

		// double rad = 0.7;
		// double locDensity = (pedsInRange) / (rad * rad * Math.PI);
		// double weidmannFlow = Math.max(
		// 0,
		// locDensity * vmax
		// * (1 - Math.exp(-1.913 * (1 / locDensity - 1 / 7))));
		// double weidmannVelocity = vdes;
		// if (locDensity > 0) {
		// weidmannVelocity = vmax;//weidmannFlow / locDensity;
		// }

		// perform the computational steps that define this equation model
		position[0] = -totalGradient.x * (speed[0]);
		position[1] = -totalGradient.y * (speed[0]);

		speed[0] = (totalGradient.getLength() * vdes - speed[0])
				* acc;
		speed[1] = 0;

		// store data
		setPosition(personCounter, yDot, position);
		setVelocity(personCounter, yDot, speed);
	}

	private Vector2D g(Vector2D gradient) {
		double normgrad = gradient.getLength();
		if (normgrad > 0) {
			// smoothly transfer from 0 to 1, by using
			// "e moll(norm,1,3) * norm + (1-e moll(norm,1,3))"
			double moll = Math.E * MathUtil.cutExp(normgrad, 1, 3);
			double newnorm = moll * normgrad + (1 - moll);
			double newX = gradient.x / normgrad * newnorm;
			double newY = gradient.y / normgrad * newnorm;
			return new Vector2D(newX, newY);
		} else {
			return gradient;
		}
	}

	@Override
	public void setVelocity(int personID, double[] solution, double[] velocity) {
		// only the first component is used in the GNM. However, the velocity given here might be in
		// vector form, since it could be given from outside of the simulator (time step file).
		if (Math.abs(velocity[1]) > 0) {
			solution[personID * dimensionPerPerson() + 2] =
					Math.sqrt(velocity[0] * velocity[0] + velocity[1] * velocity[1]);
		} else {
			// if the second component is zero, only use the first component. Note that this is
			// different from the version above since negative values also get carried over.
			solution[personID * dimensionPerPerson() + 2] = velocity[0];
		}
	}

	@Override
	public void getVelocity(int personID, double[] solution, double[] velocity) {
		velocity[0] = solution[personID * dimensionPerPerson() + 2];
		velocity[1] = 0;
	}
}
