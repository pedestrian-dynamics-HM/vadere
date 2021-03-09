package org.vadere.state.attributes.models;

import org.vadere.annotation.factories.attributes.ModelAttributeClass;
import org.vadere.state.attributes.Attributes;

import java.util.ArrayList;

@ModelAttributeClass
public class AttributesInfectionModel extends Attributes {

	private double infectionModelLastUpdateTime = -1;
	private double infectionModelUpdateStepLength = 4; // equals 1/(pedestrians' average breathing rate) in seconds
	private ArrayList<InfectionModelSourceParameters> infectionModelSourceParameters = new ArrayList<>();


	public double getInfectionModelLastUpdateTime() { return infectionModelLastUpdateTime; }

	public double getInfectionModelUpdateStepLength() { return infectionModelUpdateStepLength; }

	public ArrayList<InfectionModelSourceParameters> getInfectionModelSourceParameters() { return infectionModelSourceParameters; }

	public void setInfectionModelLastUpdateTime(double infectionModelLastUpdateTime) { this.infectionModelLastUpdateTime = infectionModelLastUpdateTime; }
}
