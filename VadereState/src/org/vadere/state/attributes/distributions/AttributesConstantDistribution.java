package org.vadere.state.attributes.distributions;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.util.reflection.VadereAttribute;

/**
 * @author Lukas Gradl (lgradl@hm.edu), Aleksandar Ivanov
 */

public class AttributesConstantDistribution extends AttributesDistribution {
	@VadereAttribute
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
