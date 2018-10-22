package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.AttributesEmbedShape;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.geometry.shapes.VShape;

/**
 * Attributes of a target area, used by TargetController in VadereSimulation.
 * 
 */
public class AttributesTarget extends AttributesEmbedShape {

	private int id = ID_NOT_SET;
	/**
	 * True: elements are removed from the simulation after entering.
	 * False: the target id is removed from the target id list, but the element remains.
	 */
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
	private double waitingTime = 0;
	/**
	 * Waiting time on the target in the yellow phase (before red and green).
	 * This can be used to cycle traffic lights in red, green or yellow phase, so that (Y -> R -> Y
	 * -> G) cycles.
	 * Needed on crossings, otherwise cars bump into each other.
	 */
	private double waitingTimeYellowPhase = 0;
	/**
	 * Number of elements that can wait or be absorbed at one time in parallel on this area.
	 * If zero, an infinite amount can wait or be absorbed.
	 */
	private int parallelWaiters = 0;
	/**
	 * True: each element on the target area is treated individually.
	 * False: the target waits for "waitingTime" and then enters "no waiting mode" for the same time
	 * (and then goes back to waiting mode). See "waitingTime".
	 */
	private boolean individualWaiting = true;

	// TODO should be "reachedDistance"; agents do not necessarily get deleted/absorbed
	private double deletionDistance = 0.1;

	/**
	 * If set to false, starts with green phase (nonblocking), otherwise blocks the path (red
	 * phase).
	 */
	private boolean startingWithRedLight = false;

	/**
	 * If non-negative, determines the desired speed the particle (pedestrian, car) is assigned
	 * after passing this target.
	 * Can be used to model street networks with differing maximal speeds on roads.
	 */
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

}
