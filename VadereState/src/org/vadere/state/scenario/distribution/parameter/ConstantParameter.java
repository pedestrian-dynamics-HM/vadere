package org.vadere.state.scenario.distribution.parameter;

/**
 * @author Lukas Gradl (lgradl@hm.edu), Aleksandar Ivanov
 */

public class ConstantParameter {
	double updateFrequency;

	public void setUpdateFrequency(double updateFrequency){
		this.updateFrequency = updateFrequency;
	}

	public double getUpdateFrequency(){
		return this.updateFrequency;
	}
}
