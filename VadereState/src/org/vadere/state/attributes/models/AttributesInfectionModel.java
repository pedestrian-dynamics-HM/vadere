package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

import java.util.ArrayList;

@ModelAttributeClass
public class AttributesInfectionModel extends Attributes {

	private double infectionModelLastUpdateTime = -1;
	private double infectionModelUpdateStepLength = 4; // equals 1/(pedestrians' average breathing rate) in seconds

	private ArrayList<InfectionModelSourceParameters> infectionModelSourceParameters = new ArrayList<>();

	private double pedestrianPathogenEmissionCapacity = 1; // emitted pathogen (in particles) = 10^pedestrianPathogenEmissionCapacity

	// Idea: use distributions and draw from org.vadere.state.scenario.ConstantDistribution
	// with defined distribution parameters similarly to "interSpawnTimeDistribution"
	/**
	 * percentage of pathogen load that is absorbed by an agent that inhales aerosol cloud with certain pathogen load
	 * 1 breath : 0.5 * 10^-3 m^3, aerosolCloud volume: 4/3 * pi * r^3 m^3 (for r=0.75m -> V=1.8 m^3)
	 * 1 breath makes about 0.03%
	 * measures such as face covering may reduce this ratio
	 */
	private double pedestrianPathogenAbsorptionRate = 0.0003;
	/**
	 * min absorbed pathogen load that leads to change in infectionStatus: susceptible -> exposed
	 * (could be defined individually for each agent depending on its immune system)
	 *
	 *
	 */
	private double pedestrianSusceptibility = 500; // particles required for being infected
	private double exposedPeriod = 2*24*60*60;
	private double infectiousPeriod = 14*24*60*60;
	private double recoveredPeriod = 150*24*60*60;

	private double aerosolCloudLifeTime = 2*60*60;
	private double aerosolCloudInitialRadius = 0.75;

	public double getInfectionModelLastUpdateTime() { return infectionModelLastUpdateTime; }

	public double getInfectionModelUpdateStepLength() { return infectionModelUpdateStepLength; }

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

	public double getAerosolCloudLifeTime() {
		return aerosolCloudLifeTime;
	}

	public double getAerosolCloudInitialRadius() {
		return aerosolCloudInitialRadius;
	}

	public void setInfectionModelLastUpdateTime(double infectionModelLastUpdateTime) { this.infectionModelLastUpdateTime = infectionModelLastUpdateTime; }
}
