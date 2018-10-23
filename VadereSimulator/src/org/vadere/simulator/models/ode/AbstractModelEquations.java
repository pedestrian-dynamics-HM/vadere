package org.vadere.simulator.models.ode;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Topography;
import org.vadere.simulator.models.potential.solver.gradients.GradientProvider;

/**
 * Abstract model equations for an ODE based model of pedestrian motion.
 * Implements the apache {@link FirstOrderDifferentialEquations} interface and
 * can thus be used with their integrator methods.
 * 
 */
public abstract class AbstractModelEquations<T extends DynamicElement> implements
		FirstOrderDifferentialEquations {
	protected GradientProvider staticGradientProvider;
	protected PotentialFieldObstacle obstacleGradientProvider;
	protected PotentialFieldAgent pedestrianGradientProvider;
	protected int Npersons;
	protected List<T> elements;
	protected Map<Integer, Integer> IDmapping;
	protected Topography topography;

	/**
	 * The dimensions for each person, i.e. position (x,y), speed / velocity...
	 */
	protected abstract int dimensionPerPerson();

	public void setGradients(GradientProvider staticGradientProvider,
			PotentialFieldObstacle potentialFieldObstacle,
			PotentialFieldAgent potentialFieldPedestrian,
			Topography scenario) {
		this.staticGradientProvider = staticGradientProvider;
		this.obstacleGradientProvider = potentialFieldObstacle;
		this.pedestrianGradientProvider = potentialFieldPedestrian;
		this.topography = scenario;
	}

	public void setElements(Collection<T> elements) {
		this.elements = new LinkedList<>(elements);
		this.Npersons = elements.size();
		this.IDmapping = new HashMap<Integer, Integer>();

		int pedCounter = 0;
		for (DynamicElement element : elements) {
			this.IDmapping.put(element.getId(), pedCounter);
			pedCounter++;
		}
	}

	/**
	 * @Return (number of persons) * (dimension per person)
	 */
	@Override
	public int getDimension() {
		return this.Npersons * dimensionPerPerson();
	}

	/**
	 * Stores the 2-dimensional position of the given person p in the given
	 * array x.
	 * 
	 * @param personID
	 * @param solution
	 * @param position
	 */
	public void getPosition(int personID, double[] solution, double[] position) {
		position[0] = solution[personID * dimensionPerPerson() + 0];
		position[1] = solution[personID * dimensionPerPerson() + 1];
	}

	/**
	 * Stores the 2-dimensional position of the given person "personID" in the
	 * given array "position".
	 * 
	 * @param personID
	 *        ID of the pedestrian.
	 * @param solution
	 *        the solution of the ODE system, containing the positions of
	 *        all pedestrians.
	 * @param position
	 *        the result is stored here.
	 */
	public void setPosition(int personID, double[] solution, double[] position) {
		solution[personID * dimensionPerPerson() + 0] = position[0];
		solution[personID * dimensionPerPerson() + 1] = position[1];
	}

	public abstract void getVelocity(int personID, double[] solution,
			double[] velocity);

	public abstract void setVelocity(int personID, double[] solution,
			double[] velocity);

	public int ID2Counter(int id) {
		return this.IDmapping.get(id);
	}

}
