package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

/**
 * The model types representing strategy, tactics and operation models.
 * 
 */
@ModelAttributeClass
public class AttributesSTOM extends Attributes {
	// TODO [priority=medium] the following strings are class names of models. better names would be operationModel, ...
	private final String operation = "org.vadere.simulator.models.gnm.GradientNavigationModel";
	private final String tactics = null;
	private final String strategy = null;

	// Getters...
	public String getOperation() {
		return operation;
	}

	public String getTactics() {
		return tactics;
	}

	public String getStrategy() {
		return strategy;
	}

}
