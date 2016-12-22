package org.vadere.state.attributes.processor;

/**
 * @author Mario Teixeira Parente
 *
 */

public class AttributesPedestrianDensityCountingProcessor extends AttributesPedestrianDensityProcessor {
	private double radius;

	public double getRadius() {
		return this.radius;
	}

	public void setRadius(double radius) {
		checkSealed();
		this.radius = radius;
	}
}
