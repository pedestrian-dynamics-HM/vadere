package org.vadere.state.attributes.processor;

/**
 * @author Mario Teixeira Parente
 *
 */

public class AttributesPedestrianDensityGaussianProcessor extends AttributesPedestrianDensityProcessor {
	private double scale = 10;
	private double standardDeviation = 0.7;
	private boolean obstacleDensity = true;

	public double getScale() {
		return scale;
	}

	public double getStandardDeviation() {
		return standardDeviation;
	}

	public boolean isObstacleDensity() {
		return obstacleDensity;
	}

	public void setScale(double scale) {
		checkSealed();
		this.scale = scale;
	}

	public void setStandardDeviation(double standardDeviation) {
		checkSealed();
		this.standardDeviation = standardDeviation;
	}

	public void setObstacleDensity(boolean obstacleDensity) {
		checkSealed();
		this.obstacleDensity = obstacleDensity;
	}
}
