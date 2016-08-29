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

}