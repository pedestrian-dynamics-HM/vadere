package org.vadere.state.scenario.distribution.parameter;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.distributions.AttributesDistribution;

/**
 * @author Lukas Gradl (lgradl@hm.edu), Aleksandar Ivanov
 */

public class AttributesConstantDistribution extends AttributesDistribution {
	Double updateFrequency;

	public AttributesConstantDistribution(){
		this(0.0);
	}
	public AttributesConstantDistribution(double updateFrequency){
		this.updateFrequency = updateFrequency;
	}
	public void setUpdateFrequency(double updateFrequency){
		this.updateFrequency = updateFrequency;
	}

	public double getUpdateFrequency(){
		return this.updateFrequency;
	}
}
