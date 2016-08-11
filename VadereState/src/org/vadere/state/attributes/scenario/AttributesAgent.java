package org.vadere.state.attributes.scenario;

/**
 * Provides attributes for an agent, like body radius, ...
 * 
 * TODO [priority=low] Create two Attributes for better performance: Common
 * Attributes and individual Attributes for pedestrians.
 * 
 * 
 */
// @JsonPropertyOrder({"radius", "densityDependentSpeed", "speedDistributionMean",
// "speedDistributionStandardDeviation", "minimumSpeed", "maximumSpeed", "acceleration", "id"}) //
// in order to have id from superclass last, to match the GSON-order
public class AttributesAgent extends AttributesDynamicElement {

	private double radius = 0.195;
	private boolean densityDependentSpeed = false;
	private double speedDistributionMean = 1.34;
	private double speedDistributionStandardDeviation = 0;
	private double minimumSpeed = 0.3;
	private double maximumSpeed = 3.0;
	private double acceleration = 2.0;

	public AttributesAgent() {
		super(-1);
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



	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		AttributesAgent that = (AttributesAgent) o;

		if (Double.compare(that.acceleration, acceleration) != 0)
			return false;
		if (densityDependentSpeed != that.densityDependentSpeed)
			return false;
		if (Double.compare(that.maximumSpeed, maximumSpeed) != 0)
			return false;
		if (Double.compare(that.minimumSpeed, minimumSpeed) != 0)
			return false;
		if (Double.compare(that.radius, radius) != 0)
			return false;
		if (Double.compare(that.speedDistributionMean, speedDistributionMean) != 0)
			return false;
		if (Double.compare(that.speedDistributionStandardDeviation, speedDistributionStandardDeviation) != 0)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		temp = Double.doubleToLongBits(radius);
		result = (int) (temp ^ (temp >>> 32));
		result = 31 * result + (densityDependentSpeed ? 1 : 0);
		temp = Double.doubleToLongBits(speedDistributionMean);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(speedDistributionStandardDeviation);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(minimumSpeed);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(maximumSpeed);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(acceleration);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
}
