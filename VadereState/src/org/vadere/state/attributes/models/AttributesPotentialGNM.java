package org.vadere.state.attributes.models;

import org.vadere.state.attributes.Attributes;

/**
 * Provides potential attributes for pedestrians and obstacles in the Gradient Navigation Model.
 * 
 */
public class AttributesPotentialGNM extends Attributes {

	private double pedestrianBodyPotential = 2.72;
	private double pedestrianRecognitionDistance = 0.8;

	private double obstacleBodyPotential = 20.1;
	private double obstacleRepulsionStrength = 0.25;

	public AttributesPotentialGNM() {}

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
