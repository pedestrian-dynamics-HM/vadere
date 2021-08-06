package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.state.health.InfectionStatus;

import java.util.ArrayList;
import java.util.Arrays;

import org.vadere.state.scenario.AerosolCloud;
import org.vadere.state.scenario.Droplets;
import org.vadere.state.health.HealthStatus;
import org.vadere.state.scenario.Pedestrian;

/**
 * This class defines the attributes of the corresponding TransmissionModel. All attributes are defined by the user and
 * relate to
 * <ul>
 *     <li>the TransmissionModel: {@link #transmissionModelSourceParameters}, {@link #pedestrianRespiratoryCyclePeriod}</li>
 *     <li>the {@link HealthStatus} of the {@link Pedestrian}s</li>
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
	private double aerosolCloudInitialArea;

	// Droplet related attributes
	private double dropletsExhalationFrequency;
	private double dropletsDistanceOfSpread;
	private double dropletsAngleOfSpreadInDeg;
	private double dropletsLifeTime;
	private double dropletsPathogenLoadFactor;

	public AttributesTransmissionModel() {
		this.transmissionModelSourceParameters = new ArrayList<>(Arrays.asList(new TransmissionModelSourceParameters(AttributesEmbedShape.ID_NOT_SET, InfectionStatus.SUSCEPTIBLE)));
		this.pedestrianRespiratoryCyclePeriod = 4; // respiratory cycle in seconds
		this.pedestrianPathogenEmissionCapacity = 4;
		this.pedestrianPathogenAbsorptionRate = 0.0005; // tidal volume (per breath) 0.5 l in m^3
		this.pedestrianMinInfectiousDose = 1000; // pathogen load required for changing infectionStatus to exposed
		this.exposedPeriod = 2 * 24 * 60 * 60; // 2 days in seconds
		this.infectiousPeriod = 14 * 24 * 60 * 60; // 14 days in seconds
		this.recoveredPeriod = 365 * 24 * 60 * 60; // 1 year in seconds
		this.aerosolCloudHalfLife = 120; // 2 minutes in seconds
		this.aerosolCloudInitialArea = 3.14; // equivalent to 1 m radius for circular aerosolClouds
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

	public double getAerosolCloudInitialArea() {
		return aerosolCloudInitialArea;
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
