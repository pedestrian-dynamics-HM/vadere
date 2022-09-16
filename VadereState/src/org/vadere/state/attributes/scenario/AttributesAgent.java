package org.vadere.state.attributes.scenario;

import org.vadere.util.reflection.VadereAttribute;

/**
 * Provides attributes for an agent, like body radius, ...
 * 
 * TODO [priority=low] Create two Attributes for better performance: Common
 * Attributes and individual Attributes for pedestrians.
 */
public class AttributesAgent extends AttributesDynamicElement {

	// from weidmann-1992 page 18, deviates in seitz-2016c page 2 (Methods): 2.0
	private Double radius = 0.2;
	// use a weidmann speed adjuster, this is not implemented jet => only densityDependentSpeed = false works.
	private Boolean densityDependentSpeed = false;
	// from weidmann-1992 page 18, seitz-2016c page 2 (Methods)
	private Double speedDistributionMean = 1.34;
	// from weidmann-1992 page 18, seitz-2016c page 2 (Methods)
	private Double speedDistributionStandardDeviation = 0.26;
	// from weidmann-1992 page 18, seitz-2016c page 2 (Methods)
	private Double minimumSpeed = 0.5;
	// from weidmann-1992 page 18, deviates in seitz-2016c page 2 (Methods): 2.0
	private Double maximumSpeed = 2.2;
	// only used for the GNM and SFM
	private Double acceleration = 2.0;
	// store n last foot steps for speed calculation
	private Integer footstepHistorySize = 4;
	// agents search for other scenario elements (e.g., other agents) within this radius
	private Double searchRadius = 1.0;
	/* angle3D in degree which is used to decide if two pedestrians move into the same direction, for instance:
	 *
	 * <pre>
	 *     T2 o   o T1
	 *        ^   ^
	 *         \a/
	 *          x
	 *         / \
	 *     P1 o   o P2
	 *
	 *     T1: target of pedestrian 1
	 *     T2: target of pedestrian 2
	 *     P1: pedestrian 1
	 *     P2: pedestrian 2
	 *     a : angle3D between the two vectors
	 * </pre>
	 *
	 * If the calculated angle3D "a" is equal or below this threshold, it is assumed that both pedestrians move into
	 * the same direction and both cannot be swapped.
	 */
	private Double walkingDirectionSameIfAngleLessOrEqual = 45.0;

	// Use "BY_TARGET_CENTER" as default because it is supported by all locomotion models.
	// "BY_TARGET_CLOSEST_POINT" might be a fragile solution because the closest point between
	// the agent and the target varies while the agent moves.
	// "BY_GRADIENT" should be the most realistic configuration because it represents the
	// instantaneous walking direction.
	private WalkingDirectionCalculation walkingDirectionCalculation = WalkingDirectionCalculation.BY_TARGET_CENTER;

	// Calculate agent's walking direction by using different strategies.
	// "BY_GRADIENT" can only be used in conjunction with locomotion models
	// which uses a floor field (which provides a gradient). "BY_TARGET_CENTER"
	// and "BY_TARGET_CLOSEST_POINT" is supported by all locomotion models.
	@VadereAttribute(exclude = true)
	public enum WalkingDirectionCalculation {
		@VadereAttribute(exclude = true)
		BY_GRADIENT,
		@VadereAttribute(exclude = true)
		BY_TARGET_CENTER,
		@VadereAttribute(exclude = true)
		BY_TARGET_CLOSEST_POINT
	}

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
		this.footstepHistorySize = other.footstepHistorySize;
		this.searchRadius = other.searchRadius;
		this.walkingDirectionCalculation = other.walkingDirectionCalculation;
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

	public int getFootstepHistorySize() { return footstepHistorySize; }

	public double getSearchRadius() { return searchRadius; }

	public WalkingDirectionCalculation getWalkingDirectionCalculation() { return walkingDirectionCalculation; }

	public double getWalkingDirectionSameIfAngleLessOrEqual() { return walkingDirectionSameIfAngleLessOrEqual; }

	// Setters...

	public void setRadius(double radius) {
		checkSealed();
		this.radius = radius;
	}

	public void setDensityDependentSpeed(boolean densityDependentSpeed) {
		checkSealed();
		this.densityDependentSpeed = densityDependentSpeed;
	}

	public void setSpeedDistributionMean(double speedDistributionMean) {
		checkSealed();
		this.speedDistributionMean = speedDistributionMean;
	}

	public void setSpeedDistributionStandardDeviation(double speedDistributionStandardDeviation) {
		checkSealed();
		this.speedDistributionStandardDeviation = speedDistributionStandardDeviation;
	}

	public void setMinimumSpeed(double minimumSpeed) {
		checkSealed();
		this.minimumSpeed = minimumSpeed;
	}

	public void setMaximumSpeed(double maximumSpeed) {
		checkSealed();
		this.maximumSpeed = maximumSpeed;
	}

	public void setAcceleration(double acceleration) {
		checkSealed();
		this.acceleration = acceleration;
	}

	public void setFootstepHistorySize(int footstepHistorySize) {
		checkSealed();
		this.footstepHistorySize = footstepHistorySize;
	}

	public void setSearchRadius(double searchRadius) {
		checkSealed();
		this.searchRadius = searchRadius;
	}

	public void setWalkingDirectionCalculation(WalkingDirectionCalculation walkingDirectionCalculation) {
		checkSealed();
		this.walkingDirectionCalculation = walkingDirectionCalculation;
	}

	public void setWalkingDirectionSameIfAngleLessOrEqual(double walkingDirectionSameIfAngleLessOrEqual) {
		checkSealed();
		this.walkingDirectionSameIfAngleLessOrEqual = walkingDirectionSameIfAngleLessOrEqual;
	}
}
