package org.vadere.state.attributes.distributions;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lukas Gradl (lgradl@hm.edu)
 */

public class AttributesEmpiricalDistribution extends AttributesDistribution {
	List<Double> values = new ArrayList<>();

	public List<Double> getValues() {
		return values;
	}

	public void setValues(List<Double> values) {
		this.values = values;
	}
}
