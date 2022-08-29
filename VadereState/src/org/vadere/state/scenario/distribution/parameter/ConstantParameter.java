package org.vadere.state.scenario.distribution.parameter;

/**
 * This is the parameter structure used with a constant distribution.
 * @author Lukas Gradl (lgradl@hm.edu), Aleksandar Ivanov, Ludwig Jaeck
 */
public class ConstantParameter {
	/**
	 * The attribute updateFrequency describes the time it takes for the next event to happen.
	 */
	double updateFrequency;

	public void setUpdateFrequency(double updateFrequency){
		this.updateFrequency = updateFrequency;
	}

	public double getUpdateFrequency(){
		return this.updateFrequency;
	}
}
