package org.vadere.state.attributes.scenario;

import com.fasterxml.jackson.annotation.JsonView;

import com.fasterxml.jackson.databind.JsonNode;
import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.state.util.Views;
import org.vadere.util.geometry.shapes.VShape;

/**
 * Attributes of a target area, used by TargetController in VadereSimulation.
 * 
 */
public class AttributesTarget extends AttributesEmbedShape {

	public static final String CONSTANT_DISTRIBUTION = "constant";
	public static final JsonNode CONSTANT_DISTRIBUTION_PAR = StateJsonConverter.createObjectNode().put("updateFrequency", 1.0);

	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private int id = ID_NOT_SET;
	/**
	 * True: elements are removed from the simulation after entering.
	 * False: the target id is removed from the target id list, but the element remains.
	 */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private boolean absorbing = true;
	/** Shape and position. */
	private VShape shape;
	/**
	 * Waiting time on the target in the yellow phase (before red and green).
	 * This can be used to cycle traffic lights in red, green or yellow phase, so that (Y -> R -> Y
	 * -> G) cycles.
	 * Needed on crossings, otherwise cars bump into each other.
	 */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private double waitingTimeYellowPhase = 0;
	/**
	 * Number of elements that can wait or be absorbed at one time in parallel on this area.
	 * If zero, an infinite amount can wait or be absorbed.
	 */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private int parallelWaiters = 0;

	/**
	 *  Modes :
	 *  "individual",
	 *  "trafficLight"
	 */
	@JsonView(Views.CacheViewExclude.class)
	private String waitingBehaviour = "individual";

	// TODO should be "reachedDistance"; agents do not necessarily get deleted/absorbed
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private double deletionDistance = 0.1;

	/**
	 * If set to false, starts with green phase (nonblocking), otherwise blocks the path (red
	 * phase).
	 */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private boolean startingWithRedLight = false;

	/**
	 * If non-negative, determines the desired speed the particle (pedestrian, car) is assigned
	 * after passing this target.
	 * Can be used to model street networks with differing maximal speeds on roads.
	 */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private double nextSpeed = -1.0;

	/**
	 *  Distribution types:
	 *  "binomial",
	 *  "constant",
	 *  "empirical",
	 *  "linearInterpolation",
	 *  "mixed",
	 *  "negativeExponential",
	 *  "normal",
	 *  "poisson",
	 *  "singleSpawn",
	 *  "timeSeries"
	 */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private String waitingTimeDistribution = CONSTANT_DISTRIBUTION;

	/**
	 *  Distribution parameter examples:
	 *  "binomial" -> "trials": 1 , "p": 0.5
	 *  "constant" -> "updateFrequency": 1.0
	 *  "empirical" -> "values" : [0.2,0.5,1.4]
	 *  "linearInterpolation", "spawnFrequency": 1.0, "xValues": [1.4, 2.4], "yValues": [5,9]
	 *  "mixed",
	 *  "negativeExponential" -> "mean": 2.4
	 *  "normal" -> "mean":1.3, "sd":0.2
	 *  "poisson" -> "numberPedsPerSecond" : 5.4
	 *  "singleSpawn", "spawnTime" : 3.0
	 *  "timeSeries" -> "intervalLength":1.2, "spawnsPerInterval" : [2,0,0,2,0,0]
	 */
	private JsonNode distributionParameters = CONSTANT_DISTRIBUTION_PAR;

	public AttributesTarget() {}

	public AttributesTarget(final VShape shape) {
		this.shape = shape;
	}

	public AttributesTarget(final VShape shape, final int id, final boolean absorbing) {
		this.shape = shape;
		this.id = id;
		this.absorbing = absorbing;
	}

	public AttributesTarget(Pedestrian pedestrian) {
		this.shape = pedestrian.getShape();
		this.absorbing = true;
		this.id = pedestrian.getIdAsTarget();
		this.waitingTimeYellowPhase = 0;
		this.parallelWaiters = 0;
		this.waitingBehaviour = "individual";
		this.startingWithRedLight = false;
		this.nextSpeed = -1;
	}

	// Getters...

	public boolean isAbsorbing() {
		return absorbing;
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

	public double getWaitingTimeYellowPhase() {
		return waitingTimeYellowPhase;
	}

	public int getParallelWaiters() {
		return parallelWaiters;
	}

	/**
	 * Within this distance, pedestrians have reached the target. It is actually not a "deletion"
	 * distance but a "reached" distance. Pedestrians do not necessarily get deleted. They can have
	 * further targets.
	 */
	public double getDeletionDistance() {
		return deletionDistance;
	}

	public boolean isStartingWithRedLight() {
		return startingWithRedLight;
	}

	public double getNextSpeed() {
		return nextSpeed;
	}

	public void setReachedDistance(double reachedDistance) {
		checkSealed();
		this.deletionDistance = reachedDistance;
	}

	public void setId(int id) {
		checkSealed();
		this.id = id;
	}

	public void setAbsorbing(boolean absorbing) {
		checkSealed();
		this.absorbing = absorbing;
	}

	public void setWaitingTimeYellowPhase(double waitingTimeYellowPhase) {
		checkSealed();
		this.waitingTimeYellowPhase = waitingTimeYellowPhase;
	}

	public void setParallelWaiters(int parallelWaiters) {
		checkSealed();
		this.parallelWaiters = parallelWaiters;
	}

	public void setDeletionDistance(double deletionDistance) {
		checkSealed();
		this.deletionDistance = deletionDistance;
	}

	public void setStartingWithRedLight(boolean startingWithRedLight) {
		checkSealed();
		this.startingWithRedLight = startingWithRedLight;
	}

	public void setNextSpeed(double nextSpeed) {
		checkSealed();
		this.nextSpeed = nextSpeed;
	}

	public JsonNode getDistributionParameters() {
		return distributionParameters;
	}

	public void setDistributionParameters(JsonNode distributionParameters) {
		checkSealed();
		this.distributionParameters = distributionParameters;
	}

	public String getWaitingTimeDistribution() {
		return waitingTimeDistribution;
	}

	public void setWaitingTimeDistribution(String waitingTimeDistribution) {
		checkSealed();
		this.waitingTimeDistribution = waitingTimeDistribution;
	}

	public Target.WaitingBehaviour getWaitingBehaviour() {
		if ("individual".equals(waitingBehaviour)) {
			return Target.WaitingBehaviour.Individual;
		} else if ("trafficLight".equals(waitingBehaviour)) {
			return Target.WaitingBehaviour.TrafficLight;
		}
		throw new IllegalArgumentException("expected a waiting mode in AttributesTarget");
	}

	public void setWaitingBehaviour(Target.WaitingBehaviour behaviour){
		checkSealed();
		if (behaviour == Target.WaitingBehaviour.Individual){
			waitingBehaviour = "individual";
		} else {
			waitingBehaviour = "trafficLight";
		}
	}
}
