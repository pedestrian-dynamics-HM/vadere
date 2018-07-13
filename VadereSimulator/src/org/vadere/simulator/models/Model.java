package org.vadere.simulator.models;

import java.util.List;
import java.util.Random;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.exceptions.AttributesMultiplyDefinedException;
import org.vadere.state.attributes.exceptions.AttributesNotFoundException;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.events.ElapsedTimeEvent;
import org.vadere.state.events.Event;
import org.vadere.state.scenario.Topography;
import org.vadere.util.data.FindByClass;

/**
 * Interface for a simulation model.
 * The <code>initialize</code> method must be called before usage!
 * This interface defines a callbacks for the simulation loop.
 * It's implementations define the major part of the simulation model's logic.
 *
 */
public interface Model {

	/**
	 * This method initializes this model by selecting the appropriate attributes from the
	 * list and creating sub models. It also sets attributes recursively for its sub models.
	 */
	void initialize(List<Attributes> attributesList, Topography topography,
			AttributesAgent attributesPedestrian, Random random);

	void preLoop(final double simTimeInSec);

	void postLoop(final double simTimeInSec);

	void update(final double simTimeInSec);

	default void update(final List<Event> events) {
	    // In the first run, ignore everything else "ElapsedTimeEvent".
	    for (Event event : events) {
	        if (event instanceof ElapsedTimeEvent) {
	            this.update(event.getTime());
            }
        }
    }

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
