package org.vadere.state.attributes.processor;

/**
 * @author Mario Teixeira Parente
 *
 */

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

	public void setScale(double scale) {
		checkSealed();
		this.scale = scale;
	}

	public void setStandardDerivation(double standardDerivation) {
		checkSealed();
		this.standardDerivation = standardDerivation;
	}

	public void setObstacleDensity(boolean obstacleDensity) {
		checkSealed();
		this.obstacleDensity = obstacleDensity;
	}
}
