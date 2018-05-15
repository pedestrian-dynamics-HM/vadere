package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

/**
 * Provides potential attributes for pedestrians and obstacles in the Gradient Navigation Model.
 * 
 */
@ModelAttributeClass
public class AttributesPotentialGNM extends Attributes {

	private double pedestrianBodyPotential = 2.72;
	private double pedestrianRecognitionDistance = 0.8;

	private double obstacleBodyPotential = 20.1;
	private double obstacleRepulsionStrength = 0.25;

	// Getters...

	public double getPedestrianBodyPotential() {
		return pedestrianBodyPotential;
	}

	public double getPedestrianRecognitionDistance() {
		return pedestrianRecognitionDistance;
	}

	public double getObstacleBodyPotential() {
		return obstacleBodyPotential;
	}

	public double getObstacleRepulsionStrength() {
		return obstacleRepulsionStrength;
	}
}
