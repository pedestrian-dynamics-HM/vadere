package org.vadere.state.attributes.distributions;

import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.state.scenario.distribution.parameter.MixedParameterDistribution;

import java.util.ArrayList;

/**
 * This is the parameter structure used with a mixed distribution.
 * @author Lukas Gradl (lgradl@hm.edu), Ludwig Jaeck
 */

public class AttributesMixedDistribution extends AttributesDistribution {
	private ArrayList<Double> switchpoints;
	private ArrayList<MixedParameterDistribution> distributions;

	public ArrayList<Double> getSwitchpoints() {
		return switchpoints;
	}

	public void setSwitchpoints(ArrayList<Double> switchpoints) {
		this.switchpoints = switchpoints;
	}

	public ArrayList<MixedParameterDistribution> getDistributions() {
		return distributions;
	}

	public void setDistributions(ArrayList<MixedParameterDistribution> distributions) {
		this.distributions = distributions;
	}
}