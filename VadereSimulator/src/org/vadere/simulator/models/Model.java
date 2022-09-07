package org.vadere.simulator.models;

import org.vadere.simulator.control.simulation.ControllerProvider;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.exceptions.AttributesMultiplyDefinedException;
import org.vadere.state.attributes.exceptions.AttributesNotFoundException;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.util.data.FindByClass;

import java.util.List;
import java.util.Random;

/**
 * Interface for a simulation model.
 * The <code>solve</code> method must be called before usage!
 * This interface defines a callbacks for the simulation loop.
 * It's implementations define the major part of the simulation model's logic.
 *
 */
public interface Model {

	/**
	 * This method initializes this model by selecting the appropriate attributes from the
	 * list and creating sub models. It also sets attributes recursively for its sub models.
	 */
	void initialize(List<Attributes> attributesList, Domain domain,
	                AttributesAgent attributesPedestrian, Random random);


	default void registerToScenarioElementControllerEvents(ControllerProvider controllerProvider){
		//do nothing on default
	}

	void preLoop(final double simTimeInSec);

	void postLoop(final double simTimeInSec);

	void update(final double simTimeInSec);

	static <T extends Attributes> T findAttributes(List<Attributes> attributesList, final Class<T> type) {
		try {
			final T a = FindByClass.findSingleObjectOfClass(attributesList, type);
			if (a != null) {
				return a;
			}
			throw new AttributesNotFoundException(type);
		} catch (IllegalArgumentException e) {
			throw new AttributesMultiplyDefinedException(type);
		}
	}




}
