package org.vadere.state.psychology.perception.types;

import org.apache.commons.math3.util.Precision;

/**
 * Class encodes some kind of information a pedestrian knows about.
 */
public class InformationStimulus extends Stimulus {

	private String information;

	public InformationStimulus(){
		super(0.0);
	}

	public InformationStimulus(String information) {
		super(0.0);
		this.information = information;
	}

	public InformationStimulus(double time, String information) {
		super(time);
		this.information = information;
	}

	public InformationStimulus(double time, String information, int id) {
		super(time, id);
		this.information = information;
	}

	public InformationStimulus(InformationStimulus other) {
		super(other.time);
		this.information = other.information;
	}



	public String getInformation() {
		return information;
	}


	@Override
	public Stimulus clone() {
		return new InformationStimulus(this);
	}

	@Override
	public boolean equals(Object that){
		if(this == that) return true;
		if(!(that instanceof InformationStimulus)) return false;
		InformationStimulus informationStimulus = (InformationStimulus) that;
		boolean isInformation = this.information.equals(informationStimulus.getInformation());
		return isInformation;
	}
}
