package org.vadere.state.scenario;

import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.behavior.SalientBehavior;
import org.vadere.state.events.types.Event;
import org.vadere.state.simulation.FootStep;
import org.vadere.state.simulation.VTrajectory;
import org.vadere.state.types.ScenarioElementType;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

public class Pedestrian extends Agent {

	// Constants
	public static double PEDESTRIAN_MAX_SPEED_METER_PER_SECOND = 12.0;

	// Variables
	private int idAsTarget; // TODO should actually be an attribute or a member of a subclass
	private boolean isChild; // TODO should actually be an attribute or a member of a subclass
	private boolean isLikelyInjured; // TODO should actually be an attribute or a member of a subclass
	private Event mostImportantEvent; /** Evaluated in each time step in "EventCognition". */
	private SalientBehavior salientBehavior;
	private LinkedList<Integer> groupIds; // TODO should actually be an attribute or a member of a subclass
	/**
	 * Footsteps is a list of foot steps a pedestrian made during the duration of one time step.
	 * For all non event driven models this is exactly one foot step. For the event driven update
	 * one pedestrian can move multiple times during one time step. To save memory the list of foot steps
	 * will be cleared after each completion of a time step. The output processor <tt>PedestrianStrideProcessor</tt>
	 * can write out those foot steps.
	 */
	private VTrajectory trajectory;

	private LinkedList<Integer> groupSizes;
	private Map<Class<? extends ModelPedestrian>, ModelPedestrian> modelPedestrianMap;
	private ScenarioElementType type = ScenarioElementType.PEDESTRIAN; // TODO used at all? For JSON de-/serialization? Car does NOT have this field. remove if unused!

	// Constructors
	@SuppressWarnings("unused")
	private Pedestrian() {
		// TODO constructor may be required for Jackson?
		this(new AttributesAgent());
	}

	private Pedestrian(AttributesAgent attributesPedestrian) {
		this(attributesPedestrian, new Random());
	}

	public Pedestrian(AttributesAgent attributesAgent, Random random) {
		super(attributesAgent, random);

		idAsTarget = -1;
		isChild = false;
		isLikelyInjured = false;
		mostImportantEvent = null;
		salientBehavior = SalientBehavior.TARGET_ORIENTED;
		groupIds = new LinkedList<>();
		groupSizes = new LinkedList<>();
		modelPedestrianMap = new HashMap<>();
		trajectory = new VTrajectory(attributesAgent.getFootStepsToStore());
	}

	private Pedestrian(Pedestrian other) {
		super(other);

		idAsTarget = other.idAsTarget;
		isChild = other.isChild;
		isLikelyInjured = other.isLikelyInjured;
		mostImportantEvent = other.mostImportantEvent;
		salientBehavior = other.salientBehavior;

		if (other.groupIds != null) {
			groupIds = new LinkedList<>(other.groupIds);
			groupSizes = new LinkedList<>(other.groupSizes);
		} else {
			groupIds = new LinkedList<>();
			groupSizes = new LinkedList<>();
		}

		trajectory = new VTrajectory();
		trajectory = other.trajectory;
	}

	public void clearFootSteps() {
		trajectory.clear();
	}

	public VTrajectory getFootSteps() {
		return trajectory;
	}
	// Getter
	public int getIdAsTarget() {
		return this.idAsTarget;
	}
	public boolean isChild() {
		return isChild;
	}
	public boolean isLikelyInjured() {
		return isLikelyInjured;
	}
	public Event getMostImportantEvent() { return mostImportantEvent; }
	public SalientBehavior getSalientBehavior() { return salientBehavior; }
	public LinkedList<Integer> getGroupIds() { return groupIds; }
	public LinkedList<Integer> getGroupSizes() {
		return groupSizes;
	}
	public <T extends ModelPedestrian> T getModelPedestrian(Class<? extends T> modelType) { return (T) modelPedestrianMap.get(modelType); }
	@Override
	public ScenarioElementType getType() {
		return ScenarioElementType.PEDESTRIAN;
	}

	// Setter
	public void setIdAsTarget(int id) { this.idAsTarget = id; }
	public void setChild(boolean child) {
		this.isChild = child;
	}
	public void setLikelyInjured(boolean likelyInjured) {
		this.isLikelyInjured = likelyInjured;
	}
	public void setMostImportantEvent(Event mostImportantEvent) { this.mostImportantEvent = mostImportantEvent; }
	public void setSalientBehavior(SalientBehavior salientBehavior) { this.salientBehavior = salientBehavior; }
	public void setGroupIds(LinkedList<Integer> groupIds) {
		this.groupIds = groupIds;
	}
	public void setGroupSizes(LinkedList<Integer> groupSizes) {
		this.groupSizes = groupSizes;
	}
	public <T extends ModelPedestrian> ModelPedestrian setModelPedestrian(T modelPedestrian) {
		return modelPedestrianMap.put(modelPedestrian.getClass(), modelPedestrian);
	}

	// Methods
	public boolean isTarget() {
		return this.idAsTarget != -1;
	}

	public void addGroupId(int groupId, int size){
		groupIds.add(groupId);
		groupSizes.add(size);
	}

	@Override
	public Pedestrian clone() {
		return new Pedestrian(this);
	}
}
