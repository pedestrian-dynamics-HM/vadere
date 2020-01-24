package org.vadere.simulator.models.ode;

import org.apache.commons.math3.exception.MathIllegalNumberException;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.scenario.AttributesDynamicElement;
import org.vadere.state.scenario.Car;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootStep;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.logging.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public abstract class ODEModel<T extends DynamicElement, TAttributes extends AttributesDynamicElement>
		implements MainModel {

	protected Random random;
	/**
	 * The last time this scenario was updated, in seconds.
	 */
	protected double lastSimTimeInSec;
	private FirstOrderIntegrator integrator;
	protected AbstractModelEquations<T> equations;
	protected Domain domain;
	protected TAttributes elementAttributes;

	private Logger logger = Logger.getLogger(ODEModel.class);
	private Class<T> type;

	@Deprecated
	public ODEModel(Class<T> type, Domain domain, FirstOrderIntegrator integrator,
			AbstractModelEquations<T> equations, TAttributes elementAttributes, Random random) {
		super();
		this.type = type;
		this.random = random;
		this.domain = domain;
		this.integrator = integrator;
		this.equations = equations;
		this.elementAttributes = elementAttributes;
	}

	public ODEModel() {}

	public void initializeODEModel(Class<T> type, FirstOrderIntegrator integrator,
			AbstractModelEquations<T> equations, TAttributes elementAttributes,
			Domain domain, Random random) {
		this.type = type;
		this.random = random;
		this.domain = domain;
		this.integrator = integrator;
		this.equations = equations;
		this.elementAttributes = elementAttributes;
	}

	@Override
	public void preLoop(final double simTimeInSec) {
		lastSimTimeInSec = simTimeInSec;
	}

	@Override
	public void postLoop(final double state) {}

	@Override
	public void update(final double simTimeInSec){

		// get pedestrian and car data
		Collection<T> dynamicElements = domain.getTopography().getElements(type);

		List<T> orderedDynamicElements = domain.getTopography().getElements(type).stream().collect(Collectors.toList());
		List<VPoint> positions = orderedDynamicElements.stream().map(ped -> ped.getPosition()).collect(Collectors.toList());

		double[] y;

		// if no peds are present, return
		if (!dynamicElements.isEmpty()) {

			// initialize the function vector for the equations
			y = new double[equations.getDimension()];
			// fill initial data with ped data
			double[] position = new double[2];
			double[] velocity = new double[2];
			int personID = 0;
			for (DynamicElement element : dynamicElements) {

				// extract data from ped
				VPoint pos = getPosition(element);
				VPoint local_velocity = getVelocity(element);

				position[0] = pos.getX();
				position[1] = pos.getY();
				velocity[0] = local_velocity.getX();
				velocity[1] = local_velocity.getY();

				equations.setPosition(personID, y, position);
				equations.setVelocity(personID, y, velocity);
				personID++;
			}

			// start point for integration: this.lastSimTimeInSec, i.e. last
			// integration end
			double Tstart = this.lastSimTimeInSec;
			// end point for integration: simTimeInSec, i.e. current time
			double Tend = simTimeInSec;

			// integrate the equations from start to end point
			if (Tstart < Tend) {
				// integrate the equations with initial data y. The solution is also
				// stored in y
				try {
					integrator.integrate(equations, 0, y, Tend - Tstart, y);
				} catch (MathIllegalNumberException e) {
					logger.error(e);
				}
			}

			updateElementPositions(type, simTimeInSec, domain.getTopography(), equations, y);

			for(int i = 0; i < orderedDynamicElements.size(); i++) {
				DynamicElement element = orderedDynamicElements.get(i);
				if (element.getType() == ScenarioElementType.PEDESTRIAN) {
					Pedestrian pedestrian = (Pedestrian)element;
					pedestrian.clearFootSteps();

					FootStep currentFootstep = new FootStep(positions.get(i), pedestrian.getPosition(), lastSimTimeInSec, simTimeInSec);
					pedestrian.getTrajectory().add(currentFootstep);
					pedestrian.getFootstepHistory().add(currentFootstep);
				}
			}
		}

		// reset the time
		this.lastSimTimeInSec = simTimeInSec;
	}

	/**
	 * Can be used to update the pedestrian position in an actual {@link Topography} from a given
	 * double vector.
	 * The {@link AbstractModelEquations} are used to get the correct positions from the vector.
	 */
	public static <T extends DynamicElement> void updateElementPositions(Class<T> type, double t, Topography topography, AbstractModelEquations equations, double[] y) {

		Collection<T> dynamicElements = topography.getElements(type);

		// copy the solution back to the pedestrians
		int counter = 0;
		for (DynamicElement element : dynamicElements) {

			// extract position and speed
			double[] newPosition = new double[2];
			counter = equations.ID2Counter(element.getId());
			equations.getPosition(counter, y, newPosition);

			// teleport ped if it crossed the end line
			// TODO [priority=low] [task=refactoring] refactoring since this
			// teleporter code should not be here
			if (topography.hasTeleporter()) {
				if (newPosition[0] > topography.getTeleporter()
						.getTeleporterPosition().x) {
					newPosition[0] += topography.getTeleporter()
							.getTeleporterShift().x;
				}
				if (newPosition[0] < topography.getTeleporter()
						.getTeleporterPosition().x
						+ topography.getTeleporter().getTeleporterShift().x) {
					newPosition[0] -= topography.getTeleporter()
							.getTeleporterShift().x;
				}
			}

			VPoint newPos = new VPoint(newPosition[0], newPosition[1]);

			double[] newVelocity = new double[2];
			equations.getVelocity(counter, y, newVelocity);

			// set data to ped
			VPoint oldPosition = element.getPosition();
			setPosition(element, newPos);
			topography.moveElement(element, oldPosition);

			setVelocity(element, new Vector2D(newVelocity[0], newVelocity[1]));
			counter++;
		}
	}

	private VPoint getVelocity(DynamicElement element) {
		if (element.getType() == ScenarioElementType.PEDESTRIAN) {
			return ((Pedestrian) element).getVelocity();
		}
		if (element.getType() == ScenarioElementType.CAR) {
			return ((Car) element).getVelocity();
		}
		throw new IllegalArgumentException("Element is neither a car nor a pedestrian.");
	}

	private VPoint getPosition(DynamicElement element) {
		if (element.getType() == ScenarioElementType.PEDESTRIAN) {
			return ((Pedestrian) element).getPosition();
		}
		if (element.getType() == ScenarioElementType.CAR) {
			return ((Car) element).getPosition();
		}
		throw new IllegalArgumentException("Element is neither a car nor a pedestrian.");
	}

	private static void setVelocity(DynamicElement element, Vector2D vector2d) {
		if (element.getType() == ScenarioElementType.PEDESTRIAN) {
			((Pedestrian) element).setVelocity(vector2d);
		} else if (element.getType() == ScenarioElementType.CAR) {
			((Car) element).setVelocity(vector2d);
		} else
			throw new IllegalArgumentException("Element is neither a car nor a pedestrian.");
	}

	private static void setPosition(DynamicElement element, VPoint newPos) {
		if (element.getType() == ScenarioElementType.PEDESTRIAN) {
			((Pedestrian) element).setPosition(newPos);
		} else if (element.getType() == ScenarioElementType.CAR) {
			((Car) element).setPosition(newPos);
		} else
			throw new IllegalArgumentException("Element is neither a car nor a pedestrian.");
	}
}
