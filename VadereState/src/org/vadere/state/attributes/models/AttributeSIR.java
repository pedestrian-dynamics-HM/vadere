package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

import java.util.ArrayList;

@ModelAttributeClass
public class AttributeSIR extends Attributes {

	// initial r-Value of SIR model
	private double initialR = 0.8;
	private ArrayList<Integer> infectionZoneIds = new ArrayList<>();
	private ArrayList<Integer> infectedPedestriansSourceId = new ArrayList<>();
	private double infectedPedestriansEmissionCapacity = 5.0;
	private double infectionModelLastUpdateTime = -1;
	private double infectionModelUpdateStepLength = 15; // equals 1/(pedestrians' average breathing rate)


	public double getInitialR() {
		return initialR;
	}

	public double getInfectionModelLastUpdateTime() {
		return infectionModelLastUpdateTime;
	}

	public double getInfectionModelUpdateStepLength() {
		return infectionModelUpdateStepLength;
	}

	public void setInitialR(double initialR) {
		// all attribute setter must have this check to ensure they are not changed during simulation
		checkSealed();
		this.initialR = initialR;
	}

	public void setInfectionZoneIds(ArrayList<Integer> infectionZoneIds) {
		checkSealed();
		this.infectionZoneIds = infectionZoneIds;
	}

	public void setInfectionModelLastUpdateTime(double infectionModelLastUpdateTime) {
		this.infectionModelLastUpdateTime = infectionModelLastUpdateTime;
	}

	public ArrayList<Integer> getInfectionZoneIds() {
		return infectionZoneIds;
	}

	public ArrayList<Integer> getInfectedPedestriansSourceId() {
		return infectedPedestriansSourceId;
	}

	public double getInfectedPedestriansEmissionCapacity() { return infectedPedestriansEmissionCapacity; }
}
