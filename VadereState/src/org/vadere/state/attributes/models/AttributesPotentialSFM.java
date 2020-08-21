package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

/**
 * Provides potential attributes for pedestrians and obstacles in the Social Force Model.
 * 
 */
@ModelAttributeClass
public class AttributesPotentialSFM extends Attributes {

	private double pedestrianBodyPotential = 2.1; // V_{\alpha \beta}^0, default from helbing-1995
	private double pedestrianRecognitionDistance = 0.3; // sigma, default from helbing-1995

	private double obstacleBodyPotential = 10.0; // U_{\alpha B}^0, default from helbing-1995
	private double obstacleRepulsionStrength = 0.2; // R, default from helbing-1995

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
