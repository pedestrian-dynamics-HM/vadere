package org.vadere.state.attributes.distributions;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the parameter structure used with a mixed distribution.
 * @author Lukas Gradl (lgradl@hm.edu), Ludwig Jaeck
 */

public class AttributesMixedDistribution extends AttributesDistribution {
	/**
	 * This list stores the event time points at which the MixedDistribution uses a different model
	 */
	private List<Double> switchpoints = new ArrayList<>();
	/**
	 * This list stores all the distribution models corresponding for each switchpoint
	 */
	private List<AttributesDistribution> distributions = new ArrayList<>();

	public List<Double> getSwitchpoints() {
		return switchpoints;
	}

	public void setSwitchpoints(ArrayList<Double> switchpoints) {
		this.switchpoints = switchpoints;
	}

	public List<AttributesDistribution> getDistributions() {
		return distributions;
	}

	public void setDistributions(List<AttributesDistribution> distributions) {
		this.distributions = distributions;
	}
}