package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;
import org.vadere.util.geometry.shapes.VPoint;

@ModelAttributeClass
public class AttributesOVM extends Attributes {

	private AttributesODEIntegrator attributesODEIntegrator;
	private double sensitivity = 1.0;
	private double sightDistance = 10.0;
	private double sightDistanceFactor = 1.0;
	private VPoint firstDistanceRandom = new VPoint(5, 15);
	private boolean ignoreOtherCars = true;

	public AttributesOVM() {
		attributesODEIntegrator = new AttributesODEIntegrator();
	}

	// Getters
	public double getSensitivity() {
		return sensitivity;
	}

	public boolean isIgnoreOtherCars() {
		return this.ignoreOtherCars;
	}

	public double getSightDistanceFactor() {
		return sightDistanceFactor;
	}

	public double getSightDistance() {
		return sightDistance;
	}

	public VPoint getFirstDistanceRandom() {
		return firstDistanceRandom;
	}


	public AttributesODEIntegrator getAttributesODEIntegrator() {
		return attributesODEIntegrator;
	}


}
