package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesEmbedShape;

import java.util.ArrayList;
import java.util.Arrays;

import org.vadere.state.health.TransmissionModelHealthStatus;
import org.vadere.state.scenario.AerosolCloud;
import org.vadere.state.scenario.Droplets;
import org.vadere.state.scenario.Pedestrian;

/**
 * This class defines the attributes of the corresponding exposure model. All attributes are defined by the user and
 * relate to
 * <ul>
 *     <li>the TransmissionModel: {@link #transmissionModelSourceParameters}, {@link #pedestrianRespiratoryCyclePeriod}</li>
 *     <li>the {@link TransmissionModelHealthStatus} of the {@link Pedestrian}s</li>
 *     <li>the {@link AerosolCloud}s' initial attributes when they are created by the TransmissionModel</li>
 *     <li>the {@link Droplets}s' initial attributes when they are created by the TransmissionModel</li>
 * </ul>
 */
@ModelAttributeClass
public class AttributesTransmissionModel extends Attributes {

	// Attributes that are required by the TransmissionModel
	private ArrayList<TransmissionModelSourceParameters> transmissionModelSourceParameters;
	private double pedestrianRespiratoryCyclePeriod; // equals 1/(pedestrians' average breathing rate) in seconds

	// Pedestrians' healthStatus related attributes
	private double pedestrianPathogenEmissionCapacity;
	private double pedestrianPathogenAbsorptionRate;
	private double pedestrianMinInfectiousDose;
	private double exposedPeriod;
	private double infectiousPeriod;
	private double recoveredPeriod;

	// AerosolCloud related attributes
	private double aerosolCloudHalfLife;
	private double aerosolCloudInitialRadius;

	// Droplet related attributes
	private double dropletsExhalationFrequency;
	private double dropletsDistanceOfSpread;
	private double dropletsAngleOfSpreadInDeg;
	private double dropletsLifeTime;
	private double dropletsPathogenLoadFactor;

	public AttributesTransmissionModel() {
		this.transmissionModelSourceParameters = new ArrayList<>(Arrays.asList(new TransmissionModelSourceParameters(AttributesEmbedShape.ID_NOT_SET, false)));

		// Mean pedestrian healthStatus and aerosolCloud attributes, rahn-2021b-cdyn Table I;
		// Some of these values are defined as mean values but could/should be introduced as distributions
		this.pedestrianRespiratoryCyclePeriod = 4; // in seconds
		this.pedestrianPathogenEmissionCapacity = 4; // pathogen particles per exhalation, logarithmized to base 10
		this.pedestrianPathogenAbsorptionRate = 0.0005; // tidal volume in m^3 per inhalation
		this.pedestrianMinInfectiousDose = 3200; // in particles
		this.exposedPeriod = 2.59E5; // in seconds
		this.infectiousPeriod = 3.46E5; // in seconds
		this.recoveredPeriod = 1.56E7; // in seconds
		this.aerosolCloudHalfLife = 600; // in seconds
		this.aerosolCloudInitialRadius = 1.5; // in m

		this.dropletsExhalationFrequency = 0;  // 0 -> no droplets are exhaled
		this.dropletsDistanceOfSpread = 1.5;
		this.dropletsAngleOfSpreadInDeg = 30;
		this.dropletsLifeTime = 1.0 + 1.0E-3; // make sure that lifeTime is not a multiple of simTimeStepLength
		this.dropletsPathogenLoadFactor = 200; // pathogen load of droplets is dropletsPathogenLoadFactor times greater than
	}

	public double getPedestrianRespiratoryCyclePeriod() {
		return pedestrianRespiratoryCyclePeriod;
	}

	public ArrayList<TransmissionModelSourceParameters> getTransmissionModelSourceParameters() {
		return transmissionModelSourceParameters;
	}

	public double getPedestrianPathogenEmissionCapacity() {
		return pedestrianPathogenEmissionCapacity;
	}

	public double getPedestrianPathogenAbsorptionRate() {
		return pedestrianPathogenAbsorptionRate;
	}

	public double getPedestrianMinInfectiousDose() {
		return pedestrianMinInfectiousDose;
	}

	public double getExposedPeriod() {
		return exposedPeriod;
	}

	public double getInfectiousPeriod() {
		return infectiousPeriod;
	}

	public double getRecoveredPeriod() {
		return recoveredPeriod;
	}

	public double getAerosolCloudHalfLife() {
		return aerosolCloudHalfLife;
	}

	public double getAerosolCloudInitialRadius() {
		return aerosolCloudInitialRadius;
	}

	public double getDropletsExhalationFrequency() {
		return dropletsExhalationFrequency;
	}

	public double getDropletsDistanceOfSpread() {
		return dropletsDistanceOfSpread;
	}

	public double getDropletsAngleOfSpreadInDeg() {
		return dropletsAngleOfSpreadInDeg;
	}

	public double getDropletsLifeTime() {
		return dropletsLifeTime;
	}

	public double getDropletsPathogenLoadFactor() {
		return dropletsPathogenLoadFactor;
	}
}
