package org.vadere.state.attributes.scenario;

import com.fasterxml.jackson.annotation.JsonView;

import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.util.Views;
import org.vadere.util.geometry.shapes.VShape;

/**
 * Attributes of a target area, used by TargetController in VadereSimulation.
 * 
 */
public class AttributesTarget extends AttributesEmbedShape {

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
	 * Waiting time in seconds on this area.
	 * If "individualWaiting" is true, then each element waits the given time on this area before
	 * "absorbing" takes place.
	 * If it is false, then the element waits this exact time before switching in "no waiting" mode
	 * and back. This way, a traffic light can be simulated.
	 */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private double waitingTime = 0;
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
	 * True: each element on the target area is treated individually.
	 * False: the target waits for "waitingTime" and then enters "no waiting mode" for the same time
	 * (and then goes back to waiting mode). See "waitingTime".
	 */
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private boolean individualWaiting = true;

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
		this.waitingTime = 0;
		this.waitingTimeYellowPhase = 0;
		this.parallelWaiters = 0;
		this.individualWaiting = true;
		this.startingWithRedLight = false;
		this.nextSpeed = -1;
	}

	// Getters...

	public boolean isIndividualWaiting() {
		return individualWaiting;
	}

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

	public double getWaitingTime() {
		return waitingTime;
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

	public void setWaitingTime(double waitingTime) {
		checkSealed();
		this.waitingTime = waitingTime;
	}

	public void setWaitingTimeYellowPhase(double waitingTimeYellowPhase) {
		checkSealed();
		this.waitingTimeYellowPhase = waitingTimeYellowPhase;
	}

	public void setParallelWaiters(int parallelWaiters) {
		checkSealed();
		this.parallelWaiters = parallelWaiters;
	}

	public void setIndividualWaiting(boolean individualWaiting) {
		checkSealed();
		this.individualWaiting = individualWaiting;
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
}
