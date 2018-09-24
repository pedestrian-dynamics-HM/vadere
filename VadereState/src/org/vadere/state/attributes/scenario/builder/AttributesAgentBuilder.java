package org.vadere.state.attributes.scenario.builder;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAgent;

public final class AttributesAgentBuilder {
	private double radius = 0.195;
	private boolean densityDependentSpeed = false;
	// from weidmann-1992 page 18, seitz-2016c page 2 (Methods)
	private double speedDistributionMean = 1.34;
	// from weidmann-1992 page 18, seitz-2016c page 2 (Methods)
	private double speedDistributionStandardDeviation = 0.26;
	// from weidmann-1992 page 18, seitz-2016c page 2 (Methods)
	private double minimumSpeed = 0.5;
	// from weidmann-1992 page 18, deviates in seitz-2016c page 2 (Methods): 2.0
	private double maximumSpeed = 2.2;
	private double acceleration = 2.0;
	private int id = Attributes.ID_NOT_SET;

	private AttributesAgentBuilder() {
	}

	public static AttributesAgentBuilder anAttributesAgent() {
		return new AttributesAgentBuilder();
	}

	public AttributesAgentBuilder radius(double radius) {
		this.radius = radius;
		return this;
	}

	public AttributesAgentBuilder densityDependentSpeed(boolean densityDependentSpeed) {
		this.densityDependentSpeed = densityDependentSpeed;
		return this;
	}

	public AttributesAgentBuilder speedDistributionMean(double speedDistributionMean) {
		this.speedDistributionMean = speedDistributionMean;
		return this;
	}

	public AttributesAgentBuilder speedDistributionStandardDeviation(double speedDistributionStandardDeviation) {
		this.speedDistributionStandardDeviation = speedDistributionStandardDeviation;
		return this;
	}

	public AttributesAgentBuilder minimumSpeed(double minimumSpeed) {
		this.minimumSpeed = minimumSpeed;
		return this;
	}

	public AttributesAgentBuilder maximumSpeed(double maximumSpeed) {
		this.maximumSpeed = maximumSpeed;
		return this;
	}

	public AttributesAgentBuilder acceleration(double acceleration) {
		this.acceleration = acceleration;
		return this;
	}

	public AttributesAgentBuilder id(int id) {
		this.id = id;
		return this;
	}

	public AttributesAgent build() {
		AttributesAgent attributesAgent = new AttributesAgent(id);
		attributesAgent.setRadius(radius);
		attributesAgent.setDensityDependentSpeed(densityDependentSpeed);
		attributesAgent.setSpeedDistributionMean(speedDistributionMean);
		attributesAgent.setSpeedDistributionStandardDeviation(speedDistributionStandardDeviation);
		attributesAgent.setMinimumSpeed(minimumSpeed);
		attributesAgent.setMaximumSpeed(maximumSpeed);
		attributesAgent.setAcceleration(acceleration);
		return attributesAgent;
	}
}
