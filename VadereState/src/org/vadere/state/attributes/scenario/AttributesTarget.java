package org.vadere.state.attributes.scenario;

import com.fasterxml.jackson.annotation.JsonView;

import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.util.Views;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.reflection.VadereAttribute;
/**
 * Attributes of a {@link Target}.
 * @author Ludwig Jaeck
 */
public class AttributesTarget extends AttributesVisualElement {
	private Boolean absorbing;
	private AttributesAbsorbingArea absorbingArea;
	private Boolean waiting;
	private AttributesWaitingArea waitingArea;

	private Double leavingSpeed;
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
	private String waitingBehaviour = Target.WaitingBehaviour.NO_WAITING.toString();
/*
	private Double waitingTimeYellowPhase = 0.0;
	/**
	 * Number of elements that can wait or be absorbed at one time in parallel on this area.
	 * If zero, an infinite amount can wait or be absorbed.
	 */
	@VadereAttribute
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private Integer parallelWaiters = 0;


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
	 *  <li>"singleSpawn"
	 *  <li>"timeSeries"
	 *  </ul>
	 *//*
	 @VadereAttribute
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private VDistribution waitingTimeDistribution;
*/

	/*
	@JsonIgnore
	private JsonNode distributionParameters ;

	/**
	 * If set to false, starts with green phase (nonblocking), otherwise blocks the path (red
	 * phase).
	 */
			/*
	@VadereAttribute
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private Boolean startingWithRedLight = false;

	/**
	 * If non-negative, determines the desired speed the particle (pedestrian, car) is assigned
	 * after passing this target.
	 * Can be used to model street networks with differing maximal speeds on roads.
	 */
			/*
	@VadereAttribute
	@JsonView(Views.CacheViewExclude.class) // ignore when determining if floor field cache is valid
	private Double nextSpeed = -1.0;

*/

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
		this.parallelWaiters = 0;
		this.waitingBehaviour = Target.WaitingBehaviour.NO_WAITING.toString();

	}

	// Getters...

	public boolean isAbsorbing() {
		return absorbing;
	}

	public void setAbsorbing(boolean absorbing) {
		checkSealed();
		this.absorbing = absorbing;
	}

	public int getParallelWaiters() {
		return parallelWaiters;
	}

	public void setParallelWaiters(int parallelWaiters) {
		checkSealed();
		this.parallelWaiters = parallelWaiters;
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

	public AttributesAbsorbingArea getAbsorbingAreaAttributes() {
		return absorbingArea;
	}

	public void setAbsorbingAreaAttributes(AttributesAbsorbingArea absorbingArea) {
		this.absorbingArea = absorbingArea;
	}

	public Boolean isWaiting() {
		return waiting;
	}

	public void setWaiting(Boolean waiting) {
		this.waiting = waiting;
	}

	public AttributesWaitingArea getWaitingAreaAttributes() {
		return waitingArea;
	}

	public void setWaitingAreaAttributes(AttributesWaitingArea waitingArea) {
		this.waitingArea = waitingArea;
	}

	public Double getLeavingSpeed() {
		return leavingSpeed;
	}

	public void setLeavingSpeed(Double leavingSpeed) {
		this.leavingSpeed = leavingSpeed;
	}
}
