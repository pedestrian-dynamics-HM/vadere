package org.vadere.state.attributes.scenario;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VShape;

/**
 * Attributes of a target area, used by TargetController in VadereSimulation.
 * 
 */
public class AttributesTarget extends Attributes {

	private int id = -1;
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

	public AttributesTarget(final AttributesTarget attributes, final VShape shape) {
		this.shape = shape;
		this.absorbing = attributes.absorbing;
		this.id = attributes.id;
		this.waitingTime = attributes.waitingTime;
		this.waitingTimeYellowPhase = attributes.waitingTimeYellowPhase;
		this.parallelWaiters = attributes.parallelWaiters;
		this.individualWaiting = attributes.individualWaiting;
		this.individualWaiting = attributes.startingWithRedLight;
		this.nextSpeed = attributes.nextSpeed;
	}

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (absorbing ? 1231 : 1237);
		long temp;
		temp = Double.doubleToLongBits(deletionDistance);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + id;
		result = prime * result + (individualWaiting ? 1231 : 1237);
		temp = Double.doubleToLongBits(nextSpeed);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + parallelWaiters;
		result = prime * result + ((shape == null) ? 0 : shape.hashCode());
		result = prime * result + (startingWithRedLight ? 1231 : 1237);
		temp = Double.doubleToLongBits(waitingTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(waitingTimeYellowPhase);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		AttributesTarget other = (AttributesTarget) obj;
		if (absorbing != other.absorbing)
			return false;
		if (Double.doubleToLongBits(deletionDistance) != Double
				.doubleToLongBits(other.deletionDistance))
			return false;
		if (id != other.id)
			return false;
		if (individualWaiting != other.individualWaiting)
			return false;
		if (Double.doubleToLongBits(nextSpeed) != Double
				.doubleToLongBits(other.nextSpeed))
			return false;
		if (parallelWaiters != other.parallelWaiters)
			return false;
		if (shape == null) {
			if (other.shape != null)
				return false;
		} else if (!shape.equals(other.shape))
			return false;
		if (startingWithRedLight != other.startingWithRedLight)
			return false;
		if (Double.doubleToLongBits(waitingTime) != Double
				.doubleToLongBits(other.waitingTime))
			return false;
		if (Double.doubleToLongBits(waitingTimeYellowPhase) != Double
				.doubleToLongBits(other.waitingTimeYellowPhase))
			return false;
		return true;
	}

}
