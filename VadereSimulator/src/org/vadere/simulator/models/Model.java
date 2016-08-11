package org.vadere.simulator.models;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.exceptions.AttributesMultiplyDefinedException;
import org.vadere.state.attributes.exceptions.AttributesNotFoundException;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Topography;

/**
 * Interface for a simulation model. The <code>initialize</code> method must be called before usage!
 * 
 *
 */
public interface Model {

	/**
	 * This method initializes this model by selecting the appropriate attributes from the
	 * list and creating sub models. It also sets attributes recursively for its sub models.
	 */
	void initialize(List<Attributes> attributesList, Topography topography,
			AttributesAgent attributesPedestrian, Random random);

	@SuppressWarnings("unchecked")
	public static <T extends Attributes> T findAttributes(List<Attributes> attributesList, final Class<T> type) {
		List<T> validAttributes = attributesList.stream()
				.filter(attr -> attr.getClass() == type)
				.map(attr -> (T) attr)
				.collect(Collectors.toList());

		if (validAttributes.isEmpty()) {
			throw new AttributesNotFoundException(type);
		} else if (validAttributes.size() > 1) {
			throw new AttributesMultiplyDefinedException(type);
		}

		return validAttributes.get(0);
	}

}
