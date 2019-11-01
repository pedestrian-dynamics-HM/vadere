package org.vadere.state.scenario;

import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesAgentPsychology;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.simulation.FootStep;
import org.vadere.state.simulation.FootstepHistory;
import org.vadere.state.simulation.VTrajectory;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

public class Pedestrian extends Agent {

	// Constants
	public static double PEDESTRIAN_MAX_SPEED_METER_PER_SECOND = 12.0;
	public static final double INVALID_NEXT_EVENT_TIME = -1.0;

	// Variables
	private int idAsTarget; // TODO should actually be an attribute or a member of a subclass
	private boolean isChild; // TODO should actually be an attribute or a member of a subclass
	private boolean isLikelyInjured; // TODO should actually be an attribute or a member of a subclass

	private AttributesAgentPsychology psychology;
	private LinkedList<Integer> groupIds; // TODO should actually be an attribute or a member of a subclass
	private LinkedList<Integer> groupSizes;

	/**
	 * trajectory is a list of foot steps a pedestrian made during the duration of one time step.
	 * For all non event driven models this is exactly one foot step. For the event driven update
	 * one pedestrian can move multiple times during one time step. To save memory the list of foot steps
	 * will be cleared after each completion of a time step. The output processor <tt>PedestrianStrideProcessor</tt>
	 * can write out those foot steps.
	 */
	private VTrajectory trajectory;
	/** This list stores the last n footsteps. I.e., this list is NOT cleared after each simulation loop like "trajectory" variable. */
	private transient FootstepHistory footstepHistory;

	private Map<Class<? extends ModelPedestrian>, ModelPedestrian> modelPedestrianMap;
	private ScenarioElementType type = ScenarioElementType.PEDESTRIAN; // TODO used at all? For JSON de-/serialization? Car does NOT have this field. remove if unused!

	// Constructors
	private Pedestrian() {
		// Default constructor required for JSON de-/serialization.
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
		psychology = new AttributesAgentPsychology(null, SelfCategory.TARGET_ORIENTED);
		groupIds = new LinkedList<>();
		groupSizes = new LinkedList<>();
		modelPedestrianMap = new HashMap<>();
		trajectory = new VTrajectory();
		footstepHistory = new FootstepHistory(attributesAgent.getFootstepHistorySize());
	}

	private Pedestrian(Pedestrian other) {
		super(other);

		idAsTarget = other.idAsTarget;
		isChild = other.isChild;
		isLikelyInjured = other.isLikelyInjured;

		psychology = new AttributesAgentPsychology();
		psychology.setMostImportantStimulus(other.getMostImportantStimulus());
		psychology.setSelfCategory(other.getSelfCategory());

		if (other.groupIds != null) {
			groupIds = new LinkedList<>(other.groupIds);
			groupSizes = new LinkedList<>(other.groupSizes);
		} else {
			groupIds = new LinkedList<>();
			groupSizes = new LinkedList<>();
		}

		trajectory = new VTrajectory();
		trajectory = other.trajectory;
		footstepHistory = other.footstepHistory;
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
	public Stimulus getMostImportantStimulus() { return psychology.getMostImportantStimulus(); }
	public SelfCategory getSelfCategory() { return psychology.getSelfCategory(); }
	public LinkedList<Integer> getGroupIds() { return groupIds; }
	public LinkedList<Integer> getGroupSizes() {
		return groupSizes;
	}
	public <T extends ModelPedestrian> T getModelPedestrian(Class<? extends T> modelType) { return (T) modelPedestrianMap.get(modelType); }
	@Override
	public ScenarioElementType getType() {
		return ScenarioElementType.PEDESTRIAN;
	}

	public VTrajectory getTrajectory() {
		return trajectory;
	}

	public FootstepHistory getFootstepHistory() {
		return footstepHistory;
	}

    public VPoint getInterpolatedFootStepPosition(double time){
        if(this.footstepHistory.getCapacity() <= 0){
            throw new IllegalArgumentException("Cannot interpolate foot steps if there is no capacity (larger than zero) " +
                    "for storing foot steps (see 'scenario.attributesPedestrian.footStepsToStore' field)");
        }

        FootStep currentFootStep = this.footstepHistory.getYoungestFootStep();

        if(currentFootStep == null){
            return getPosition();
        }else{
            if(time > currentFootStep.getEndTime()){
                // This happens for example if a pedestrian is waiting (see Events)
                return currentFootStep.getEnd();
            }else{
                return FootStep.interpolateFootStep(currentFootStep, time);
            }
        }
    }

	// Setter
	public void setIdAsTarget(int id) { this.idAsTarget = id; }
	public void setChild(boolean child) {
		this.isChild = child;
	}
	public void setLikelyInjured(boolean likelyInjured) {
		this.isLikelyInjured = likelyInjured;
	}
	public void setMostImportantStimulus(Stimulus mostImportantStimulus) { psychology.setMostImportantStimulus(mostImportantStimulus); }
	public void setSelfCategory(SelfCategory selfCategory) { psychology.setSelfCategory(selfCategory); }
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

	public void addFootStepToTrajectory(FootStep footStep){
		this.trajectory = this.trajectory.add(footStep);
	}

	public void clearFootSteps() {
		// getLast() is always the most recent (made sure in VTrajectory.add)
		// This statement is for security and should mostly have no effect (only if someone did not use method
		// "addFootStepToTrajectory" to add another foot step to the trajectory)
		if(!trajectory.isEmpty()){
			trajectory.clear();
		}
	}

	// Overridden Methods

	@Override
	public Pedestrian clone() {
		return new Pedestrian(this);
	}
}
