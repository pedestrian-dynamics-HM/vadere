package org.vadere.state.attributes.processors;

import org.vadere.state.attributes.Attributes;

public class AttributesDensityGaussianProcessor extends Attributes {

	private double scale = 10;

	private double standardDerivation = 0.7;

	private boolean obstacleDensity = true;

	public double getScale() {
		return scale;
	}

	public double getStandardDerivation() {
		return standardDerivation;
	}

	public boolean isObstacleDensity() {
		return obstacleDensity;
	}
}
