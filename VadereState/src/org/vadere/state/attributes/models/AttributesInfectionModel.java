package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

import java.util.ArrayList;

@ModelAttributeClass
public class AttributesInfectionModel extends Attributes {

	private ArrayList<Integer> infectedPedestriansSourceId = new ArrayList<>();
	private double infectionModelLastUpdateTime = -1;
	private double infectionModelUpdateStepLength = 15; // equals 1/(pedestrians' average breathing rate)
	private ArrayList<InfectionModelSourceParameters> infectionModelSourceParameters;


	public double getInfectionModelLastUpdateTime() { return infectionModelLastUpdateTime; }

	public double getInfectionModelUpdateStepLength() { return infectionModelUpdateStepLength; }

	public ArrayList<InfectionModelSourceParameters> getInfectionModelSourceParameters() { return infectionModelSourceParameters; }

	public ArrayList<Integer> getInfectedPedestriansSourceId() { return infectedPedestriansSourceId; }

	public void setInfectionModelLastUpdateTime(double infectionModelLastUpdateTime) { this.infectionModelLastUpdateTime = infectionModelLastUpdateTime; }
}
