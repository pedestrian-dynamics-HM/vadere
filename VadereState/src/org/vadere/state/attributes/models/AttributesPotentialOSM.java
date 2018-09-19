package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

/**
 * Provides potential attributes for pedestrians and obstacles in the Gradient Navigation Model.
 * 
 */
@ModelAttributeClass
public class AttributesPotentialOSM extends Attributes {

	// seitz-2012 u_p = 1000
	private double pedestrianBodyPotential = 1000;

	// seitz-2012 h_p = 1.0
	private double pedestrianRepulsionWidth = 1.0;

	// seitz-2012 v_p = 0.4
	private double pedestrianRepulsionStrength = 0.4;

	// seitz-2012 a_p = 1.0
	private double aPedOSM = 3.5;

	// seitz-2012 b_p = 0.2
	private double bPedOSM = 0.6;

	//
	private double pedestrianRecognitionDistance = 1.5;

	//
	private double personalDensityFactor = 1.2;

	// seitz-2012 u_o = 10000
	private double obstacleBodyPotential = 10000;

	// seitz-2012 h_o = 6.0
	private double obstacleRepulsionWidth = 6;

	// seitz-2012 v_o = 0.2
	private double obstacleRepulsionStrength = 0.2;

	// seitz-2012 a_o = 3.0
	private double aObsOSM = 3.5;

	// seitz-2012 b_0 = 2.0
	private double bObsOSM = 0.2;

	// Getters...

	public double getPedestrianBodyPotential() {
		return pedestrianBodyPotential;
	}

	public double getPedestrianRepulsionWidth() {
		return pedestrianRepulsionWidth;
	}

	public double getPedestrianRepulsionStrength() {
		return pedestrianRepulsionStrength;
	}

	public double getPedestrianRecognitionDistance() {
		return pedestrianRecognitionDistance;
	}

	public double getPersonalDensityFactor() {
		return personalDensityFactor;
	}

	public double getObstacleBodyPotential() {
		return obstacleBodyPotential;
	}

	public double getObstacleRepulsionWidth() {
		return obstacleRepulsionWidth;
	}

	public double getObstacleRepulsionStrength() {
		return obstacleRepulsionStrength;
	}

	public double getAPedOSM() {
		return aPedOSM;
	}

	public double getBPedOSM() {
		return bPedOSM;
	}

	public double getAObsOSM() {
		return aObsOSM;
	}

	public double getBObsOSM() {
		return bObsOSM;
	}
}
