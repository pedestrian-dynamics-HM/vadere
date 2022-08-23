package org.vadere.state.scenario.distribution.parameter;

/**
 * This is the parameter structure used with a constant distribution.
 * @author Lukas Gradl (lgradl@hm.edu), Aleksandar Ivanov, Ludwig Jaeck
 */

public class ConstantParameter {
	/**
	 * The attribute timeInterval describes the time it takes for the next event to happen.
	 */
	double timeInterval;

	public void setTimeInterval(double timeInterval){
		this.timeInterval = timeInterval;
	}

	public double getTimeInterval(){
		return this.timeInterval;
	}
}
