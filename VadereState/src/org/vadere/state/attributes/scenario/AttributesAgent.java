package org.vadere.state.attributes.scenario;

/**
 * Provides attributes for an agent, like body radius, ...
 * 
 * TODO [priority=low] Create two Attributes for better performance: Common
 * Attributes and individual Attributes for pedestrians.
 * 
 * 
 */
public class AttributesAgent extends AttributesDynamicElement {

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

	public AttributesAgent() {
		this(-1);
	}

	public AttributesAgent(final int id) {
		super(id);
	}

	/**
	 * Copy constructor with new id assignment.
	 */
	public AttributesAgent(final AttributesAgent other, final int id) {
		super(id);
		this.radius = other.radius;
		this.densityDependentSpeed = other.densityDependentSpeed;
		this.speedDistributionMean = other.speedDistributionMean;
		this.speedDistributionStandardDeviation = other.speedDistributionStandardDeviation;
		this.minimumSpeed = other.minimumSpeed;
		this.maximumSpeed = other.maximumSpeed;
		this.acceleration = other.acceleration;
	}

	// Getters...

	public double getRadius() {
		return radius;
	}

	public boolean isDensityDependentSpeed() {
		return densityDependentSpeed;
	}

	public double getSpeedDistributionMean() {
		return speedDistributionMean;
	}

	public double getSpeedDistributionStandardDeviation() {
		return speedDistributionStandardDeviation;
	}

	public double getMinimumSpeed() {
		return minimumSpeed;
	}

	public double getMaximumSpeed() {
		return maximumSpeed;
	}

	public double getAcceleration() {
		return acceleration;
	}

}
