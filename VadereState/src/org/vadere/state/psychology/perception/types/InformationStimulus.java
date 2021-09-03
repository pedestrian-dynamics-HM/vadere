package org.vadere.state.psychology.perception.types;

import org.apache.commons.math3.util.Precision;

/**
 * Class encodes some kind of information a pedestrian knows about.
 * The information is active from the time gien in {@link #time} and will be
 * forgotten at {@link #obsolete_at} (or never if {@link #obsolete_at} == -1
 */
public class InformationStimulus extends Stimulus {

	private String information;
	private double obsolete_at;

	public InformationStimulus(String information) {
		super(0.0);
		this.information = information;
		this.obsolete_at = -1; // never
	}

	public InformationStimulus(double time, double obsolete_at, String information) {
		super(time);
		this.information = information;
		this.obsolete_at = obsolete_at;
	}

	public InformationStimulus(double time, double obsolete_at, String information, int id) {
		super(time, id);
		this.information = information;
		this.obsolete_at = obsolete_at;
	}

	public InformationStimulus(InformationStimulus other) {
		super(other.time);
		this.information = other.information;
		this.obsolete_at  = other.obsolete_at;
	}



	public String getInformation() {
		return information;
	}


	public double getObsolete_at() {
		return obsolete_at;
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
		boolean isProb = Precision.equals(this.perceptionProbability, informationStimulus.getPerceptionProbability(), Double.MIN_VALUE);
		boolean isInformation = this.information.equals(informationStimulus.getInformation());
		boolean obsolete = Precision.equals(this.obsolete_at, informationStimulus.getObsolete_at(), Double.MIN_VALUE);
		return isProb && isInformation && obsolete;
	}
}
