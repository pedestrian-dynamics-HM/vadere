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

	// calculate the angle between agent's current position and the target by using either the center of the target or
	// the closest point between agent and target
	public enum AngleCalculationType { USE_CENTER, USE_CLOSEST_POINT };

	// from weidmann-1992 page 18, deviates in seitz-2016c page 2 (Methods): 2.0
	private double radius = 0.2;

	// use a weidmann speed adjuster, this is not implemented jet => only densityDependentSpeed = false works.
	private boolean densityDependentSpeed = false;

	// from weidmann-1992 page 18, seitz-2016c page 2 (Methods)
	private double speedDistributionMean = 1.34;

	// from weidmann-1992 page 18, seitz-2016c page 2 (Methods)
	private double speedDistributionStandardDeviation = 0.26;

	// from weidmann-1992 page 18, seitz-2016c page 2 (Methods)
	private double minimumSpeed = 0.5;

	// from weidmann-1992 page 18, deviates in seitz-2016c page 2 (Methods): 2.0
	private double maximumSpeed = 2.2;

	// only used for the GNM and SFM
	private double acceleration = 2.0;

	// store n last foot steps for speed calculation
	private int footstepHistorySize = 4;

	// agents search for other scenario elements (e.g., other agents) within this radius
	private double searchRadius = 1.0;

	// the more robust strategy should be use the target's center for angle calculation
	// because the closest point can vary while the agent moves through the topography.
	private AngleCalculationType angleCalculationType = AngleCalculationType.USE_CENTER;

	/* angle in degree which is used to decide if two pedestrians move into the same direction
	 *
	 * The angle "a" is calculated between the two vectors v1 and v2 where
	 * v1 = (TargetPedestrian1 - pedestrian1) and v2 = (TargetPedestrian2 - pedestrian2):
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
	 *     a : angle between the two vectors
	 * </pre>
	 *
	 * If the calculated angle "a" is equal or below this threshold, it is assumed that both pedestrians move into
	 * the same direction and both cannot be swapped.
	 */
	private double targetOrientationAngleThreshold = 45.0;

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
		this.angleCalculationType = other.angleCalculationType;
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

	public AngleCalculationType getAngleCalculationType() { return angleCalculationType; }

	public double getTargetOrientationAngleThreshold() { return targetOrientationAngleThreshold; }

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

	public void setAngleCalculationType(AngleCalculationType angleCalculationType) {
		checkSealed();
		this.angleCalculationType = angleCalculationType;
	}

	public void setTargetOrientationAngleThreshold(double targetOrientationAngleThreshold) {
		checkSealed();
		this.targetOrientationAngleThreshold = targetOrientationAngleThreshold;
	}
}
