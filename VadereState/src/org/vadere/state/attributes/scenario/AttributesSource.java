package org.vadere.state.attributes.scenario;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.ConstantDistribution;
import org.vadere.state.types.DynamicElementType;
import org.vadere.util.geometry.shapes.VShape;

public class AttributesSource extends Attributes {

	public static final String CONSTANT_DISTRIBUTION = ConstantDistribution.class.getName();
	public static final int NO_MAX_SPAWN_NUMBER_TOTAL = -1;

	private int id = -1;

	/** Shape and position. */
	private VShape shape = null;
	private String interSpawnTimeDistribution = CONSTANT_DISTRIBUTION;
	private List<Double> distributionParameters = Collections.singletonList(1.0);

	private int spawnNumber = 1;

	/** Maximum number of spawned elements. {@link #NO_MAX_SPAWN_NUMBER_TOTAL} -> no maximum number. */
	private int maxSpawnNumberTotal = NO_MAX_SPAWN_NUMBER_TOTAL;

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
	 * Maximum number of spawned elements. The number
	 * {@link #NO_MAX_SPAWN_NUMBER_TOTAL} means there is no maximum.
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

	public void setStartTime(double time) {
		checkSealed();
		startTime = time;
	}

	public void setEndTime(double time) {
		checkSealed();
		endTime = time;
	}

	public void setDistributionParameters(List<Double> distributionParameters) {
		checkSealed();
		this.distributionParameters = distributionParameters;
	}

	public void setInterSpawnTimeDistribution(String interSpawnTimeDistribution) {
		checkSealed();
		this.interSpawnTimeDistribution = interSpawnTimeDistribution;
	}

	public void setMaxSpawnNumberTotal(int maxSpawnNumberTotal) {
		checkSealed();
		this.maxSpawnNumberTotal = maxSpawnNumberTotal;
	}

	public void setSpawnAtRandomPositions(boolean spawnAtRandomPositions) {
		checkSealed();
		this.spawnAtRandomPositions = spawnAtRandomPositions;
	}

}
