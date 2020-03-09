package org.vadere.state.attributes.scenario;

import com.fasterxml.jackson.annotation.JsonView;

import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.state.scenario.ConstantDistribution;
import org.vadere.state.types.DynamicElementType;
import org.vadere.state.util.Views;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class AttributesSource extends AttributesEmbedShape {

	public static final String CONSTANT_DISTRIBUTION = ConstantDistribution.class.getName();
	public static final int NO_MAX_SPAWN_NUMBER_TOTAL = -1;

	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private int id = ID_NOT_SET;

	/** Shape and position. */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private VShape shape = null;
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private String interSpawnTimeDistribution = CONSTANT_DISTRIBUTION;
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private List<Double> distributionParameters = Collections.singletonList(1.0);

	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private int spawnNumber = 1;

	/** Maximum number of spawned elements. {@link #NO_MAX_SPAWN_NUMBER_TOTAL} -> no maximum number. */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private int maxSpawnNumberTotal = NO_MAX_SPAWN_NUMBER_TOTAL;

	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private double startTime = 0;
	/** endTime == startTime means one single spawn event. */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private double endTime = 0;
	
	/**
	 * The pedestrians are spawned at random positions rather than from the top
	 * left corner downwards.
	 */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private boolean spawnAtRandomPositions;


	/**
	 * If set to true, the pedestrians are spawned on a rectangular grid for the cellular automaton. Different to the
	 * regular spawnArray, they will touch if the cells are 0.4m and the radius is set to 0.2m.
	 */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private boolean spawnAtGridPositionsCA;

	/**
	 * If set to true, only free space is used to create pedestrians at each
	 * wave. When the endTime is reached and not enough pedestrians have been
	 * created yet, there will be less pedestrians than spawnNumber *
	 * (endTime-startTime)/spawnDelay in the scenario.
	 *
	 * useFreeSpaceOnly = false can cause errors if tow pedestrians arw spawned at
	 * exactly the same place. Maybe Deprecate this switch.
	 */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private boolean useFreeSpaceOnly = true;
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private List<Integer> targetIds = new LinkedList<>();

	/**
	 *  This Attribute only takes affect if a model org.vadere.simulator.models.groups.GroupModel
	 *  is present in the scenario. When this is the case this list defines the group size
	 *  distribution of this source. The list can be arbitrary long but must add up to 1.
	 *  The index of the list represents the size of the  groups and the value the probability
	 *  index 0 => GroupSize = 1
	 *  index 1 => GroupSize = 2
	 *  ...
	 *
	 *  Example: ----------------------------------------------------------------------------------
	 *  probability [ 0.0, 0.0, 0.25, 0.25, 0.25, .... ] ------------------------------------------
  	 *  GroupSize   [  1    2    3     4     5         ] ------------------------------------------
	 *  uniform distribution of groups of the size from 3 to 5 ------------------------------------
	 */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private List<Double> groupSizeDistribution = Arrays.asList(1.0);

	/**
	 * The type of dynamic elements this source creates.
	 */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private DynamicElementType dynamicElementType = DynamicElementType.PEDESTRIAN;

	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private AttributesAgent attributesPedestrian = null;
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
	 *  https://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math3/distribution/package-summary.html
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
	 * Set the {@link #endTime} to 1e9 and this attribute to 10.
	 */
	public int getMaxSpawnNumberTotal() {
		return maxSpawnNumberTotal;
	}

	public boolean isSpawnAtRandomPositions() {
		return spawnAtRandomPositions;
	}

	public boolean isSpawnAtGridPositionsCA(){ return spawnAtGridPositionsCA; }

	public AttributesAgent getAttributesPedestrian() {
		return attributesPedestrian;
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

	@Override
	public void setShape(VShape shape) {
		this.shape = shape;
	}

	@Override
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

	public List<Double> getGroupSizeDistribution() {
		return groupSizeDistribution;
	}

	public void setGroupSizeDistribution(List<Double> groupSizeDistribution) {
		checkSealed();
		this.groupSizeDistribution = groupSizeDistribution;
	}

	public void setSpawnNumber(int spawnNumber) {
		checkSealed();
		this.spawnNumber = spawnNumber;
	}

	public void setUseFreeSpaceOnly(boolean useFreeSpaceOnly) {
		checkSealed();
		this.useFreeSpaceOnly = useFreeSpaceOnly;
	}

	public void setTargetIds(List<Integer> targetIds) {
		checkSealed();
		this.targetIds = targetIds;
	}

	public void setDynamicElementType(DynamicElementType dynamicElementType) {
		checkSealed();
		this.dynamicElementType = dynamicElementType;
	}

	public void setId(int id) {
		checkSealed();
		this.id = id;
	}



}
