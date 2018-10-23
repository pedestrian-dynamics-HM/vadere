/**
 * This class manages the Equations of the Optimal Velocity Model and computes the
 * derivative-equations
 * 
 */
package org.vadere.simulator.models.ovm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.vadere.simulator.models.ode.AbstractModelEquations;
import org.vadere.simulator.models.ode.ODEModel;
import org.vadere.state.attributes.models.AttributesOVM;
import org.vadere.state.scenario.Car;
import org.vadere.state.scenario.Target;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.parallel.AParallelWorker;
import org.vadere.util.parallel.CountableParallelWorker;
import org.vadere.util.parallel.IAsyncComputable;
import org.vadere.util.parallel.AParallelWorker.Work;

public class OVMEquations extends AbstractModelEquations<Car> implements IAsyncComputable {

	private AttributesOVM attributesOVM;
	private double sensitivity;
	private double sightDistance;
	private double sightDistanceFactor;
	private boolean pedestrianInteraction;

	private final Random rand = new Random(1); // needed to create ghost cars for a time instant.

	/**
	 * Optimal Velocity Function in the form of the original paper
	 * 
	 * @param x_n Xn
	 * @param x_n_1 Xn-1
	 * @return
	 */
	public double ovFunction(double x_n, double x_n_1, double speed) {
		double dX = Math.max(0, x_n_1 - x_n);
		return speed / 2 * (Math.tanh((8 * dX) / (3.6 * speed) - 2) + Math.tanh(2));
	}

	@Override
	public void computeDerivatives(double t, double[] x, double[] xdot) {
		// fetch attributes of the model
		sensitivity = attributesOVM.getSensitivity();
		sightDistance = attributesOVM.getSightDistance();
		sightDistanceFactor = attributesOVM.getSightDistanceFactor();

		// update position of all dynamic elements
		ODEModel.updateElementPositions(Car.class, t, topography, this, x);

		// create workers for each pedestrian
		List<AParallelWorker<Double[]>> workers = new ArrayList<AParallelWorker<Double[]>>();

		// loop over all persons and compute the next step
		int personCounter = 0; // used for arrays, not identical to personID!
		for (final Car car : elements) {
			AParallelWorker<Double[]> w = new CountableParallelWorker<Double[]>(
					personCounter, new Work<Double[]>() {
						private int ID;

						@Override
						public Double[] call() throws Exception {
							computeSingleCarParallel(car, getWorkerID(), t, x, xdot);
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
				e.printStackTrace();
			} catch (InterruptedException e) {
				// Necessary in order to tell Simulation the thread has been
				// interrupted.
				// TODO [priority=medium] [task=refactoring] Hack, other solution?
				Thread.currentThread().interrupt();
				break;
			}
		}
	}


	private void computeSingleCarParallel(
			Car currentCar, int carIdInArray, double t,
			double[] x, double[] xdot) {

		int fCI = -1;
		Car nearestCar = null;

		List<Car> neighbors = topography.getSpatialMap(Car.class).getObjects(currentCar.getPosition(), sightDistance);
		if (topography.hasTeleporter()) {
			Vector2D tpoint = topography.getTeleporter().getTeleporterShift();
			List<Car> ghostCars =
					topography.getSpatialMap(Car.class).getObjects(currentCar.getPosition().add(tpoint), sightDistance);
			for (Car ghost : ghostCars) {
				Car repositionedGhost = new Car(ghost.getAttributes(), rand);
				repositionedGhost.setPosition(
						ghost.getPosition().add(topography.getTeleporter().getTeleporterShift().multiply(-1)));
				neighbors.add(repositionedGhost);
			}
		}

		for (int j = 0; j < neighbors.size(); j++) {
			Car car = neighbors.get(j);

			if (nearestCar == null) {
				if (car != currentCar && isInFront(car, currentCar)) {
					nearestCar = car;
					fCI = j;
				}
			} else {
				if (car != currentCar &&
						isInFront(car, currentCar) &&
						isInFront(nearestCar, car)) {
					nearestCar = car;
					fCI = j;
				}
			}
		}

		// Check Pedestrians if pedestrianIteraction is turned on
		if (pedestrianInteraction) {
			// TODO [priority=medium] [task=bugfix] [Error?]
		}

		// evaluate the front car with respect to a limited sight distance
		/*
		 * List<Car> neighbors =
		 * topography.getSpatialMap(Car.class).getObjects(actualCar.getPosition(), sightDistance);
		 * for(Car car:neighbors){
		 * if (nearestCar == null){
		 * nearestCar=car;
		 * } else {
		 * if (car.getPosition().getX() > actualCar.getPosition().getX()
		 * && car.getPosition().getX() < nearestCar.getPosition().getX()){
		 * nearestCar=car;
		 * }
		 * }
		 * }
		 */
		computeSingleCar(currentCar, nearestCar, t, x, xdot, carIdInArray, fCI);

	}

	private boolean isInFront(Car car, Car currentCar) {
		if (this.attributesOVM.isIgnoreOtherCars())
			return false;

		if (currentCar.hasNextTarget() && car.hasNextTarget()) {
			// TODO [priority=low] [task=feature] check that the cars actually stop when another
			// car is in front
			if (currentCar.getNextTargetId() != car.getNextTargetId()) {
				return false;
			}

			Target target = topography.getTarget(currentCar.getNextTargetId());
			if (target == null) {
				System.out.println(currentCar.getNextTargetId());
			}
			double distanceToThis = target.getShape().distance(currentCar.getPosition());
			double distanceToOther = target.getShape().distance(car.getPosition());
			return distanceToOther < distanceToThis;
		} else {
			return car.getPosition().getX() >= currentCar.getPosition().getX();
		}
	}

	/**
	 * Computation of speed and position of a single car for one time step
	 * 
	 * @param currentCar
	 * @param frontCar
	 * @param t
	 * @param x
	 * @param xdot
	 * @param index
	 */
	private void computeSingleCar(Car currentCar, Car frontCar, double t, double[] x, double[] xdot, int index,
			int fCI) {

		double[] position = new double[2];
		double[] speed = new double[2];
		double[] position2 = new double[2];

		getPosition(index, x, position2);
		getVelocity(index, x, speed);

		VPoint myPos = new VPoint(position2[0], position2[1]);
		Vector2D myVelocity = new Vector2D(speed[0], speed[1]);
		double mySpeed = myVelocity.x; // only the x coordinate is set. Do NOT use "length" here, otherwise negative speeds occur.

		// velocity is just passed through, but rotated so that the vector points in target
		// direction
		if (currentCar.hasNextTarget()) {
			position[0] = myVelocity.x;
			position[1] = myVelocity.y;
		}

		// also change the direction of the acceleration so that the velocity for each car is
		// correctly set.
		if (currentCar.hasNextTarget()) {
			Target target = topography.getTarget(currentCar.getNextTargetId());
			VPoint closest = new Vector2D(target.getShape().getBounds2D().getCenterX(),
					target.getShape().getBounds2D().getCenterY());
			double localspeed = mySpeed;
			Vector2D direction = new Vector2D(closest.add(myPos.scalarMultiply(-1))).normalize(localspeed);
			position[0] = direction.x;
			position[1] = direction.y;
		}

		if (frontCar == null) {
			double newTargetX = sightDistance * sightDistanceFactor;

			if (currentCar.hasNextTarget()) // only one target left (?)
			{
				Target target = topography.getTarget(currentCar.getNextTargetId());
				if (target == null) {
					System.out.println(currentCar.getNextTargetId());
				}
				if (target.getShape().distance(myPos) < sightDistance) {
					newTargetX = myPos.distance(new VPoint(target.getShape().getBounds2D().getCenterX(),
							target.getShape().getBounds2D().getCenterY()));
				}
			}
			speed[0] = sensitivity * ovFunction(0.0, newTargetX, currentCar.getFreeFlowSpeed()) - sensitivity * mySpeed;
		} else {
			speed[0] = sensitivity * ovFunction(0.0,
					myPos.distance(frontCar.getPosition()) - currentCar.getAttributes().getLength() * 2,
					currentCar.getFreeFlowSpeed()) - sensitivity * myVelocity.getLength();
		}
		speed[1] = 0;


		setPosition(index, xdot, position);
		setVelocity(index, xdot, speed);
	}

	@Override
	/**
	 * Dimension per Car
	 * 4 parameters: position (x,y), velocity (x,y)
	 */
	protected int dimensionPerPerson() {
		return 4;
	}

	public void setVelocity(int personID, double[] solution, double[] velocity) {
		solution[personID * dimensionPerPerson() + 2] = velocity[0];
		solution[personID * dimensionPerPerson() + 3] = velocity[1];
	}

	@Override
	public void getVelocity(int personID, double[] solution, double[] velocity) {
		velocity[0] = solution[personID * dimensionPerPerson() + 2];
		velocity[1] = solution[personID * dimensionPerPerson() + 3];
	}

	public void setModelAttributes(AttributesOVM attributes) {
		this.attributesOVM = attributes;
	}

	public void setPedestrianInteraction(boolean pedestrianInteraction) {
		this.pedestrianInteraction = pedestrianInteraction;

	}

}
