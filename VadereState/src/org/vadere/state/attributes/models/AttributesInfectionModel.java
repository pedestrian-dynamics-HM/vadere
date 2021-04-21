package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

import java.util.ArrayList;

@ModelAttributeClass
public class AttributesInfectionModel extends Attributes {

	private ArrayList<InfectionModelSourceParameters> infectionModelSourceParameters = new ArrayList<>();

	private double pedestrianRespiratoryCyclePeriod = 4; // equals 1/(pedestrians' average breathing rate) in seconds
	private double pedestrianPathogenEmissionCapacity = 4; // 10^pedestrianPathogenEmissionCapacity = emitted pathogen (in particles)

	// Idea: use distributions and draw from org.vadere.state.scenario.ConstantDistribution
	// with defined distribution parameters similarly to "interSpawnTimeDistribution"
	/**
	 * pedestrianPathogenAbsorptionRate: tidal volume in m^3; one could account for protective measures such as masks
	 * by multiplying the tidal volume by a "mask efficiency factor [0, 1]"
	 */
	private double pedestrianPathogenAbsorptionRate = 0.0005;
	/**
	 * min absorbed pathogen load that leads to change in infectionStatus: susceptible -> exposed
	 * (could be defined individually for each agent depending on its immune system)
	 */
	private double pedestrianSusceptibility = 1000; // absorbed particles required for being exposed
	private double exposedPeriod = 2*24*60*60;
	private double infectiousPeriod = 14*24*60*60;
	private double recoveredPeriod = 365*24*60*60;

	private double aerosolCloudHalfLife = 60;
	private double aerosolCloudInitialArea = 1.7;

	public double getPedestrianRespiratoryCyclePeriod() { return pedestrianRespiratoryCyclePeriod; }

	public ArrayList<InfectionModelSourceParameters> getInfectionModelSourceParameters() { return infectionModelSourceParameters; }

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