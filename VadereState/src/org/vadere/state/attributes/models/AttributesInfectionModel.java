package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

import java.util.ArrayList;

@ModelAttributeClass
public class AttributesInfectionModel extends Attributes {

	private double infectionModelLastUpdateTime = -1;
	private double infectionModelUpdateStepLength = 4; // equals 1/(pedestrians' average breathing rate) in seconds

	private ArrayList<InfectionModelSourceParameters> infectionModelSourceParameters = new ArrayList<>();

	// Idea: use distributions and draw from org.vadere.state.scenario.ConstantDistribution
	// with defined distribution parameters similarly to "interSpawnTimeDistribution"

	/**
	 * percentage of pathogen load that is absorbed by an agent that inhales aerosol cloud with certain pathogen load
	 */
	private double pedestrianPathogenAbsorptionRate = 0.1;
	/**
	 * min absorbed pathogen load that leads to susceptible -> exposed (could be defined individually for each agent
	 * depending on its immune system)
	 */
	private double pedestrianSusceptibility = 1;
	private double exposedPeriod = 2*24*60*60;
	private double infectiousPeriod = 14*24*60*60;
	private double recoveredPeriod = 150*24*60*60;

	private double aerosolCloudLifeTime = 2*60*60;
	private double aerosolCloudInitialRadius = 0.75;

	public double getInfectionModelLastUpdateTime() { return infectionModelLastUpdateTime; }

	public double getInfectionModelUpdateStepLength() { return infectionModelUpdateStepLength; }

	public ArrayList<InfectionModelSourceParameters> getInfectionModelSourceParameters() { return infectionModelSourceParameters; }

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

	public double getAerosolCloudLifeTime() {
		return aerosolCloudLifeTime;
	}

	public double getAerosolCloudInitialRadius() {
		return aerosolCloudInitialRadius;
	}

	public void setInfectionModelLastUpdateTime(double infectionModelLastUpdateTime) { this.infectionModelLastUpdateTime = infectionModelLastUpdateTime; }
}
