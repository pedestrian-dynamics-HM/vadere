package org.vadere.state.scenario.distribution.parameter;

import org.vadere.state.attributes.distributions.AttributesDistribution;

import java.util.ArrayList;

/**
 * @author Lukas Gradl (lgradl@hm.edu)
 */

public class AttributesMixedDistribution extends AttributesDistribution {
	private Double[] switchpoints;
	private ArrayList<MixedParameterDistribution> distributions;

	public Double[] getSwitchpoints() {
		return switchpoints;
	}

	public void setSwitchpoints(Double[] switchpoints) {
		this.switchpoints = switchpoints;
	}

	public ArrayList<MixedParameterDistribution> getDistributions() {
		return distributions;
	}

	public void setDistributions(ArrayList<MixedParameterDistribution> distributions) {
		this.distributions = distributions;
	}
}