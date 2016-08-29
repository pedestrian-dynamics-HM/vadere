package org.vadere.state.attributes.scenario;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.ConstantDistribution;
import org.vadere.state.types.DynamicElementType;
import org.vadere.util.geometry.shapes.VShape;

public class AttributesSource extends Attributes {

	public static final String CONSTANT_DISTRIBUTION = ConstantDistribution.class.getName();

	private int id;

	/** Shape and position. */
	private VShape shape = null;
	@Deprecated
	private double spawnDelay = -1; // see getSpawnDelay()
	private String interSpawnTimeDistribution = CONSTANT_DISTRIBUTION;
	private List<Double> distributionParameters = Arrays.asList(new Double[] {1.0});

	private int spawnNumber = 1;

	/** Maximum number of spawned elements. 0 -> no maximum number. */
	private int maxSpawnNumberTotal = 0;

	private double startTime = 0;
	/** endTime == startTime means one single spawn event. */
	private double endTime = 0;
	
	/**
	 * The pedestrians are spawned at random positions rather than from the top
	 * left corner downwards.
	 */
	private boolean spawnAtRandomPositions;
	/**
	 * If set to true, only free space is used to create pedestrians at each
	 * wave. When the endTime is reached and not enough pedestrians have been
	 * created yet, there will be less pedestrians than spawnNumber *
	 * (endTime-startTime)/spawnDelay in the scenario.
	 */
	private boolean useFreeSpaceOnly;
	private List<Integer> targetIds = new LinkedList<>();
	/**
	 * The type of dynamic elements this source creates.
	 */
	private DynamicElementType dynamicElementType = DynamicElementType.PEDESTRIAN;

	/**
	 * This (private) default constructor is used by Gson. Without it, the initial field assignments
	 * above have no effect. In other words, no default values for fields are possible without a
	 * default constructor.
	 */
	@SuppressWarnings("unused")
	private AttributesSource() {}

	public AttributesSource(int id) {
		this.id = id;
	}

	public AttributesSource(int id, VShape shape) {
		this.id = id;
		this.shape = shape;
	}

	// Getters...

	/**
	 * Still used for constant spawn time algorithm. This property will be deleted in favor of
	 * <code>distributionParameters</code>.
	 * 
	 * @deprecated Use {@link #getDistributionParameters()} instead.
	 */
	@Deprecated
	public double getSpawnDelay() {
		// use spawn delay from distribution parameter list if possible
		if (interSpawnTimeDistribution.equals(CONSTANT_DISTRIBUTION)
				&& spawnDelay == -1) {
			return distributionParameters.get(0);
		}
		return spawnDelay;
	}

	/**
	 * Class name of distribution for inter-spawn times. The name must point to a subclass of
	 * {@link org.apache.commons.math3.distribution.RealDistribution}. This subclass must have at
	 * least one public constructor with the following arguments: 1.
	 * {@link org.apache.commons.math3.random.RandomGenerator},
	 * 2. one or more arguments of type <code>double</code> for distribution parameters.
	 * 
	 * @see Class#getName()
	 * @see https://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math3/distribution/package-summary.html
	 */
	public String getInterSpawnTimeDistribution() {
		return interSpawnTimeDistribution;
	}

	public List<Double> getDistributionParameters() {
		return distributionParameters;
	}

	/** Get number of pedestrians to be spawned at one point in time. */
	public int getSpawnNumber() {
		return spawnNumber;
	}

	public double getStartTime() {
		return startTime;
	}

	/** If end time equals start time, exactly one single spawn event will be triggered. */
	public double getEndTime() {
		return endTime;
	}

	/**
	 * Maximum number of spawned elements. The number 0 means there is no
	 * maximum.
	 * 
	 * This attribute can be used together with non-constant distributions. For
	 * example, consider an exponential distribution. The times of events are
	 * random. How to ensure, that exactly 10 elements are spawned? Solution:
	 * Set the {@link endTime} to 1e9 and this attribute to 10.
	 */
	public int getMaxSpawnNumberTotal() {
		return maxSpawnNumberTotal;
	}

	public boolean isSpawnAtRandomPositions() {
		return spawnAtRandomPositions;
	}

	public boolean isUseFreeSpaceOnly() {
		return useFreeSpaceOnly;
	}

	public List<Integer> getTargetIds() {
		return targetIds;
	}

	public int getId() {
		return id;
	}

	public VShape getShape() {
		return shape;
	}

	public DynamicElementType getDynamicElementType() {
		return dynamicElementType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((distributionParameters == null) ? 0 : distributionParameters.hashCode());
		result = prime * result
				+ ((dynamicElementType == null) ? 0 : dynamicElementType.hashCode());
		long temp;
		temp = Double.doubleToLongBits(endTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + id;
		result = prime * result + ((interSpawnTimeDistribution == null) ? 0
				: interSpawnTimeDistribution.hashCode());
		result = prime * result + ((shape == null) ? 0 : shape.hashCode());
		result = prime * result + (spawnAtRandomPositions ? 1231 : 1237);
		temp = Double.doubleToLongBits(spawnDelay);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + spawnNumber;
		temp = Double.doubleToLongBits(startTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((targetIds == null) ? 0 : targetIds.hashCode());
		result = prime * result + (useFreeSpaceOnly ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AttributesSource other = (AttributesSource) obj;
		if (distributionParameters == null) {
			if (other.distributionParameters != null)
				return false;
		} else if (!distributionParameters.equals(other.distributionParameters))
			return false;
		if (dynamicElementType != other.dynamicElementType)
			return false;
		if (Double.doubleToLongBits(endTime) != Double.doubleToLongBits(other.endTime))
			return false;
		if (id != other.id)
			return false;
		if (interSpawnTimeDistribution == null) {
			if (other.interSpawnTimeDistribution != null)
				return false;
		} else if (!interSpawnTimeDistribution.equals(other.interSpawnTimeDistribution))
			return false;
		if (shape == null) {
			if (other.shape != null)
				return false;
		} else if (!shape.equals(other.shape))
			return false;
		if (spawnAtRandomPositions != other.spawnAtRandomPositions)
			return false;
		if (Double.doubleToLongBits(spawnDelay) != Double.doubleToLongBits(other.spawnDelay))
			return false;
		if (spawnNumber != other.spawnNumber)
			return false;
		if (Double.doubleToLongBits(startTime) != Double.doubleToLongBits(other.startTime))
			return false;
		if (targetIds == null) {
			if (other.targetIds != null)
				return false;
		} else if (!targetIds.equals(other.targetIds))
			return false;
		if (useFreeSpaceOnly != other.useFreeSpaceOnly)
			return false;
		return true;
	}

}
