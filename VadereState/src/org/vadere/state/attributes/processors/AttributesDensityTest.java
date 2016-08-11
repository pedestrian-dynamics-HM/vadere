package org.vadere.state.attributes.processors;

import org.vadere.state.attributes.Attributes;

public class AttributesDensityTest extends Attributes {
	private double maxDensity = 6.0;
	private double minDensity = 0.0;
	private double maxMeanDensity = 6.0;
	private double minMeanDensity = 0.0;
	private boolean expectFailure = false;

	public double getMaxDensity() {
		return maxDensity;
	}

	public double getMaxMeanDensity() {
		return maxMeanDensity;
	}

	public double getMinMeanDensity() {
		return minMeanDensity;
	}

	public double getMinDensity() {
		return minDensity;
	}

	public boolean isExpectFailure() {
		return expectFailure;
	}
}
