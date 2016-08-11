package org.vadere.simulator.projects.dataprocessing_mtp;

public class AttributesPedestrianDensityGaussianProcessor extends AttributesPedestrianDensityProcessor {
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
