package org.vadere.state.scenario.distribution.parameter;

/**
 * This is the parameter structure used with an empirical distribution.
 * @author Lukas Gradl (lgradl@hm.edu)
 */

public class EmpiricalParameter {
	double[] values;

	public double[] getValues() {
		return values;
	}

	public void setValues(double[] values) {
		this.values = values;
	}
}
