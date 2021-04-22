package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.state.health.InfectionStatus;

import java.util.ArrayList;
import java.util.Arrays;

@ModelAttributeClass
public class AttributesInfectionModel extends Attributes {

	private ArrayList<InfectionModelSourceParameters> infectionModelSourceParameters;

	private double pedestrianRespiratoryCyclePeriod; // equals 1/(pedestrians' average breathing rate) in seconds
	private double pedestrianPathogenEmissionCapacity; // 10^pedestrianPathogenEmissionCapacity = emitted pathogen (in particles)

	// Idea: use distributions and draw from org.vadere.state.scenario.ConstantDistribution
	// with defined distribution parameters similarly to "interSpawnTimeDistribution"
	/**
	 * pedestrianPathogenAbsorptionRate: tidal volume in m^3; one could account for protective measures such as masks
	 * by multiplying the tidal volume by a "mask efficiency factor [0, 1]"
	 */
	private double pedestrianPathogenAbsorptionRate;
	/**
	 * min absorbed pathogen load that leads to change in infectionStatus: susceptible -> exposed
	 * (could be defined individually for each agent depending on its immune system)
	 */
	private double pedestrianSusceptibility; // absorbed particles required for being exposed
	private double exposedPeriod;
	private double infectiousPeriod;
	private double recoveredPeriod;

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
