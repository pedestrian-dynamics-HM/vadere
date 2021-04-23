package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.state.health.InfectionStatus;

import java.util.ArrayList;
import java.util.Arrays;

import org.vadere.state.scenario.AerosolCloud;
import org.vadere.state.health.HealthStatus;
import org.vadere.state.scenario.Pedestrian;

/**
 * This class defines the attributes of the corresponding InfectionModel. All attributes are defined by the user and
 * relate to
 * <ul>
 *     <li>the InfectionModel: {@link #infectionModelSourceParameters}, {@link #pedestrianRespiratoryCyclePeriod}</li>
 *     <li>the {@link HealthStatus} of the {@link Pedestrian}s</li>
 *     <li>the {@link AerosolCloud}s' initial attributes when they are created by the InfectionModel</li>
 * </ul>
 */
@ModelAttributeClass
public class AttributesInfectionModel extends Attributes {

	// Attributes that are required by the InfectionModel
	private ArrayList<InfectionModelSourceParameters> infectionModelSourceParameters;
	private double pedestrianRespiratoryCyclePeriod; // equals 1/(pedestrians' average breathing rate) in seconds

	// Pedestrians' healthStatus related attributes
	private double pedestrianPathogenEmissionCapacity;
	private double pedestrianPathogenAbsorptionRate;
	private double pedestrianSusceptibility;
	private double exposedPeriod;
	private double infectiousPeriod;
	private double recoveredPeriod;

	// AerosolCloud related attributes
	private double aerosolCloudHalfLife;
	private double aerosolCloudInitialArea;

	public AttributesInfectionModel() {
		this.infectionModelSourceParameters = new ArrayList<>(Arrays.asList(new InfectionModelSourceParameters(AttributesEmbedShape.ID_NOT_SET, InfectionStatus.SUSCEPTIBLE)));
		this.pedestrianRespiratoryCyclePeriod = 4;
		this.pedestrianPathogenEmissionCapacity = 4;
		this.pedestrianPathogenAbsorptionRate = 0.0005;
		this.pedestrianSusceptibility = 1000;
		this.exposedPeriod = 2 * 24 * 60 * 60;
		this.infectiousPeriod = 14 * 24 * 60 * 60;
		this.recoveredPeriod = 365 * 24 * 60 * 60;
		this.aerosolCloudHalfLife = 60;
		this.aerosolCloudInitialArea = 0.75;
	}

	public double getPedestrianRespiratoryCyclePeriod() {
		return pedestrianRespiratoryCyclePeriod;
	}

	public ArrayList<InfectionModelSourceParameters> getInfectionModelSourceParameters() {
		return infectionModelSourceParameters;
	}

	public double getPedestrianPathogenEmissionCapacity() {
		return pedestrianPathogenEmissionCapacity;
	}

	public double getPedestrianPathogenAbsorptionRate() {
		return pedestrianPathogenAbsorptionRate;
	}

	public double getPedestrianSusceptibility() {
		return pedestrianSusceptibility;
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
}
