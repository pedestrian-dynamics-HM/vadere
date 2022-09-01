package org.vadere.state.scenario.distribution.parameter;

import org.vadere.state.attributes.distributions.AttributesDistribution;

import java.util.Arrays;

/**
 * @author Lukas Gradl (lgradl@hm.edu)
 */

public class AttributesEmpiricalDistribution extends AttributesDistribution {
	Double[] values;

	public Double[] getValues() {
		return values;
	}

	public void setValues(Double[] values) {
		this.values = values;
	}
}
