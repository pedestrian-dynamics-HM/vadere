package org.vadere.state.scenario.distribution.parameter;

import java.util.ArrayList;

/**
 * @author Lukas Gradl (lgradl@hm.edu)
 */

public class MixedParameter {
	private double[] switchpoints;
	private ArrayList<MixedParameterDistribution> distributions;

	public double[] getSwitchpoints() {
		return switchpoints;
	}

	public void setSwitchpoints(double[] switchpoints) {
		this.switchpoints = switchpoints;
	}

	public ArrayList<MixedParameterDistribution> getDistributions() {
		return distributions;
	}

	public void setDistributions(ArrayList<MixedParameterDistribution> distributions) {
		this.distributions = distributions;
	}
}