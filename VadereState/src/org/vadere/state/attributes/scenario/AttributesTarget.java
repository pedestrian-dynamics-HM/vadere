package org.vadere.state.attributes.scenario;

import com.fasterxml.jackson.annotation.JsonView;

import com.fasterxml.jackson.databind.JsonNode;
import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.util.Views;
import org.vadere.state.scenario.distribution.parameter.*;
import org.vadere.util.geometry.shapes.VShape;

/**
 * Attributes of a {@link Target}.
 * @author Ludwig Jaeck
 */
public class AttributesTarget extends AttributesEmbedShape {
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private int id = ID_NOT_SET;


	/** Shape and position. */
	private VShape shape;

	/**
	 * True: elements are removed from the simulation after entering.
	 * False: the target id is removed from the target id list, but the element remains.
	 */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private boolean absorbing = true;

	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private double deletionDistance = 0.1;


	/**
	 *  Modes: <br>
	 *  <ul>
	 *      <li>INDIVIDUAL</li>
	 *      <p>Agents have an individual waiting time at the target. The waiting by time is described by {@link AttributesTarget#waitingTimeDistribution}.</p>
	 *      <li>TRAFFIC_LIGHT</li>
	 *      <p>The target is a traffic light. Agents wait for the green phase.</p>
	 *      <li>NO_WAITING</li>
	 *      <p>Agents are switching target immediately of 'absorbing' is set to false else the get absorbed.</p>
	 *  </ul>
	 */
	@JsonView(Views.CacheViewExclude.class)
	private String waitingBehaviour;

	/**
	 * Number of elements that can wait or be absorbed at one time in parallel on this area.
	 * If zero, an infinite amount can wait or be absorbed.
	 */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private int parallelWaiters = 0;

	// TODO should be "reachedDistance"; agents do not necessarily get deleted/absorbed

	/**
	 *  Distribution types:<br>
	 *  <ul>
	 *  <li>"binomial"
	 *  <li>"constant"
	 *  <li>"empirical"
	 *  <li>"linearInterpolation"
	 *  <li>"mixed"
	 *  <li>"negativeExponential"
	 *  <li>"normal"
	 *  <li>"poisson"
	 *  <li>"singleEvent"
	 *  <li>"timeSeries"
	 *  </ul>
	 */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private String waitingTimeDistribution;

	/**
	 *  Distribution types:<br>
	 *  <ul>
	 *  <li>"BinomialParameter"				[{@link BinomialParameter}]</li>
	 *  <li>"ConstantParameter"				[{@link ConstantParameter}]</li>
	 *  <li>"EmpiricalParameter"			[{@link EmpiricalParameter}]</li>
	 *  <li>"LinearInterpolationParameter"	[{@link LinearInterpolationParameter}]</li>
	 *  <li>"MixedParameter"				[{@link MixedParameter}]</li>
	 *  <li>"NegativeExponentialParameter"	[{@link NegativeExponentialParameter}]</li>
	 *  <li>"NormalParameter"				[{@link NormalParameter}]</li>
	 *  <li>"PoissonParameter"				[{@link PoissonParameter}]</li>
	 *  <li>"SingleEventParameter"			[{@link SingleEventParameter}]</li>
	 *  <li>"TimeSeriesParameter"			[{@link TimeSeriesParameter}]</li>
	 *  </ul>
	 */
	private JsonNode distributionParameters ;


	/**
	 * If set to false, starts with green phase (nonblocking), otherwise blocks the path (red
	 * phase).
	 */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private boolean startingWithRedLight = false;

	/**
	 * Waiting time on the target in the yellow phase (before red and green).
	 * This can be used to cycle traffic lights in red, green or yellow phase, so that (Y -> R -> Y
	 * -> G) cycles.
	 * Needed on crossings, otherwise cars bump into each other.
	 */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private double waitingTimeYellowPhase = 0;

	/**
	 * If non-negative, determines the desired speed the particle (pedestrian, car) is assigned
	 * after passing this target.
	 * Can be used to model street networks with differing maximal speeds on roads.
	 */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private double nextSpeed = -1.0;



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
		this.waitingBehaviour = Target.WaitingBehaviour.NO_WAITING.toString();
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
		if (waitingBehaviour.equals(Target.WaitingBehaviour.INDIVIDUAL.toString())) {
			return Target.WaitingBehaviour.INDIVIDUAL;
		} else if (waitingBehaviour.equals(Target.WaitingBehaviour.TRAFFIC_LIGHT.toString())) {
			return Target.WaitingBehaviour.TRAFFIC_LIGHT;
		}
		else if (waitingBehaviour.equals(Target.WaitingBehaviour.NO_WAITING.toString())){
			return  Target.WaitingBehaviour.NO_WAITING;
		}
		throw new IllegalArgumentException("expected a waiting mode in AttributesTarget");
	}

	public void setWaitingBehaviour(Target.WaitingBehaviour behaviour){
		checkSealed();
		this.waitingBehaviour = behaviour.toString();
	}
}
