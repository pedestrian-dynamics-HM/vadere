package org.vadere.state.scenario;

import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.information.KnowledgeBase;
import org.vadere.state.psychology.PsychologyStatus;
import org.vadere.state.health.InfectionStatus;
import org.vadere.state.health.HealthStatus;
import org.vadere.state.psychology.cognition.GroupMembership;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.ThreatMemory;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.simulation.FootStep;
import org.vadere.state.simulation.FootstepHistory;
import org.vadere.state.simulation.VTrajectory;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.*;

public class Pedestrian extends Agent {

    // Constants
    public static double PEDESTRIAN_MAX_SPEED_METER_PER_SECOND = 12.0;
    public static final double INVALID_NEXT_EVENT_TIME = -1.0;

    // Variables
    // TODO: All these variables belong to Isabella's "social identity" branch
    //   which was never merged with "master". On "master", these variables should be unused.
    //   Therefore, delete them.
    private int idAsTarget; // TODO should actually be an attribute or a member of a subclass
    private boolean isChild; // TODO should actually be an attribute or a member of a subclass
    private boolean isLikelyInjured; // TODO should actually be an attribute or a member of a subclass

    private PsychologyStatus psychologyStatus;

	private HealthStatus healthStatus;

	private LinkedList<Integer> groupIds; // TODO should actually be an attribute or a member of a subclass
	private LinkedList<Integer> groupSizes;

    private LinkedList<Pedestrian> agentsInGroup = new LinkedList<>();


    /**
     * trajectory is a list of foot steps a pedestrian made during the duration of one time step.
     * For all non event driven models this is exactly one foot step. For the event driven update
     * one pedestrian can move multiple times during one time step. To save memory the list of foot steps
     * will be cleared after each completion of a time step. The output processor <tt>PedestrianStrideProcessor</tt>
     * can write out those foot steps.
     */
    private VTrajectory trajectory;
    /**
     * This list stores the last n footsteps. I.e., this list is NOT cleared after each simulation loop like "trajectory" variable.
     */
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
		psychologyStatus = new PsychologyStatus(null, new ThreatMemory(), SelfCategory.TARGET_ORIENTED, GroupMembership.OUT_GROUP, new KnowledgeBase());
		healthStatus = new HealthStatus();
		groupIds = new LinkedList<>();
		groupSizes = new LinkedList<>();
		modelPedestrianMap = new HashMap<>();
		trajectory = new VTrajectory();
		footstepHistory = new FootstepHistory(attributesAgent.getFootstepHistorySize());
	}

    protected Pedestrian(Pedestrian other) {
        super(other);

        idAsTarget = other.idAsTarget;
        isChild = other.isChild;
        isLikelyInjured = other.isLikelyInjured;

		psychologyStatus = new PsychologyStatus(other.psychologyStatus);

		healthStatus = new HealthStatus(other.healthStatus);

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

    public Stimulus getMostImportantStimulus() {
        return psychologyStatus.getMostImportantStimulus();
    }

    public LinkedList<Stimulus> getPerceivedStimuli(){ return psychologyStatus.getPerceivedStimuli(); }

    public LinkedList<Stimulus> getNextPerceivedStimuli(){ return psychologyStatus.getNextPerceivedStimuli(); }




    public ThreatMemory getThreatMemory() {
        return psychologyStatus.getThreatMemory();
    }

    public SelfCategory getSelfCategory() {
        return psychologyStatus.getSelfCategory();
    }

    public GroupMembership getGroupMembership() {
        return psychologyStatus.getGroupMembership();
    }

    public KnowledgeBase getKnowledgeBase() {
        return psychologyStatus.getKnowledgeBase();
    }

    public LinkedList<Integer> getGroupIds() {
        return groupIds;
    }

    public LinkedList<Integer> getGroupSizes() {
        return groupSizes;
    }

    public <T extends ModelPedestrian> T getModelPedestrian(Class<? extends T> modelType) {
        return (T) modelPedestrianMap.get(modelType);
    }

    @Override
    public ScenarioElementType getType() {
        return ScenarioElementType.PEDESTRIAN;
    }

    public InfectionStatus getInfectionStatus() {
    	return healthStatus.getInfectionStatus();
    }

	public double getPathogenAbsorbedLoad() {
    	return healthStatus.getPathogenAbsorbedLoad();
    }

	public VPoint getStartBreatheOutPosition() {
    	return healthStatus.getStartBreatheOutPosition();
    }

	public boolean isBreathingIn() {
    	return healthStatus.isBreathingIn();
    }

	public double getMinInfectiousDose() {
    	return healthStatus.getMinInfectiousDose();
    }


    public VTrajectory getTrajectory() {
        return trajectory;
    }

    public FootstepHistory getFootstepHistory() {
        return footstepHistory;
    }

    public VPoint getInterpolatedFootStepPosition(double time) {
        if (this.footstepHistory.getCapacity() <= 0) {
            throw new IllegalArgumentException("Cannot interpolate foot steps if there is no capacity (larger than zero) " +
                    "for storing foot steps (see 'scenario.attributesPedestrian.footStepsToStore' field)");
        }

        FootStep currentFootStep = this.footstepHistory.getYoungestFootStep();

        if (currentFootStep == null) {
            return getPosition();
        } else {
            if (time > currentFootStep.getEndTime()) {
                // This happens for example if a pedestrian is waiting (see Events)
                return currentFootStep.getEnd();
            } else {
                return FootStep.interpolateFootStep(currentFootStep, time);
            }
        }
    }

    // Setter
    public void setIdAsTarget(int id) {
        this.idAsTarget = id;
    }

    public void setChild(boolean child) {
        this.isChild = child;
    }

    public void setLikelyInjured(boolean likelyInjured) {
        this.isLikelyInjured = likelyInjured;
    }

    public void setMostImportantStimulus(Stimulus mostImportantStimulus) {
        psychologyStatus.setMostImportantStimulus(mostImportantStimulus);
    }

    public void setPerceivedStimuli(LinkedList<Stimulus> stimuli){ psychologyStatus.setPerceivedStimuli(stimuli); }

    public void setNextPerceivedStimuli(LinkedList<Stimulus> stimuli){ psychologyStatus.setNextPerceivedStimuli(stimuli); }


    public void setThreatMemory(ThreatMemory threatMemory) {
        psychologyStatus.setThreatMemory(threatMemory);
    }

    public void setSelfCategory(SelfCategory selfCategory) {
        psychologyStatus.setSelfCategory(selfCategory);
    }

    public void setGroupMembership(GroupMembership groupMembership) {
        psychologyStatus.setGroupMembership(groupMembership);
    }

    public void setGroupIds(LinkedList<Integer> groupIds) {
        this.groupIds = groupIds;
    }

    public void setGroupSizes(LinkedList<Integer> groupSizes) {
        this.groupSizes = groupSizes;
    }

    public <T extends ModelPedestrian> ModelPedestrian setModelPedestrian(T modelPedestrian) {
        return modelPedestrianMap.put(modelPedestrian.getClass(), modelPedestrian);
    }

	public void setInfectionStatus(InfectionStatus infectionStatus) {
    	healthStatus.setInfectionStatus(infectionStatus);
    }

	public void setStartBreatheOutPosition(VPoint startBreatheOutPosition) {
    	healthStatus.setStartBreatheOutPosition(startBreatheOutPosition);
    }

	public void setRespiratoryTimeOffset(double respiratoryTimeOffset) {
    	healthStatus.setRespiratoryTimeOffset(respiratoryTimeOffset);
    }

	public void setPathogenEmissionCapacity(double pathogenEmissionCapacity) {
    	healthStatus.setPathogenEmissionCapacity(pathogenEmissionCapacity);
    }

	public void setPathogenAbsorptionRate(double pathogenAbsorptionRate) {
    	healthStatus.setPathogenAbsorptionRate(pathogenAbsorptionRate);
    }

    public void setPathogenAbsorbedLoad(double absorbedLoad) {
        healthStatus.setPathogenAbsorbedLoad(absorbedLoad);
    }

	public void setMinInfectiousDose(double minInfectiousDose) {
    	healthStatus.setMinInfectiousDose(minInfectiousDose);
    }

	public void setExposedPeriod(double exposedPeriod) {
    	healthStatus.setExposedPeriod(exposedPeriod);
    }

	public void setInfectiousPeriod(double infectiousPeriod) {
    	healthStatus.setInfectiousPeriod(infectiousPeriod);
    }

	public void setRecoveredPeriod(double recoveredPeriod) {
    	healthStatus.setRecoveredPeriod(recoveredPeriod);
    }

    // Methods
    public boolean isTarget() {
        return this.idAsTarget != -1;
    }

    public void addGroupId(int groupId, int size) {
        groupIds.add(groupId);
        groupSizes.add(size);
    }

    public void addFootStepToTrajectory(FootStep footStep) {
        this.trajectory = this.trajectory.add(footStep);
    }

    public void clearFootSteps() {
        if (!trajectory.isEmpty()) {
            trajectory.clear();
        }
    }

	public double emitPathogen() {
    	return healthStatus.emitPathogen();
    }

	public void absorbPathogen(double pathogenConcentration) {
		healthStatus.absorbPathogen(pathogenConcentration);
	}

	public void updateInfectionStatus(double simTimeInSec)	{
		healthStatus.updateInfectionStatus(simTimeInSec);
	}

	public void updateRespiratoryCycle(double simTimeInSec, double periodLength) {
		healthStatus.updateRespiratoryCycle(simTimeInSec, periodLength);
	}
	public boolean isStartingBreatheOut() {
		return healthStatus.isStartingBreatheOut();
	}

	public boolean isStartingBreatheIn() {
		return healthStatus.isStartingBreatheIn();
	}

	// Overridden Methods

    @Override
    public Pedestrian clone() {
        return new Pedestrian(this);
    }


    public LinkedList<Pedestrian> getPedGroupMembers() {
        return agentsInGroup;
    }

    public boolean isAgentsInGroup() {
        return getPedGroupMembers().size() > 0;
    }

    public void setAgentsInGroup(final LinkedList<Pedestrian> agentsInGroup) {
        this.agentsInGroup = agentsInGroup;
    }

    @Override
    public void setTargets(LinkedList<Integer> target) {

        if (getGroupIds() != null) {
            if (getGroupIds().size() == 1) {
                Collection<Pedestrian> peds = getPedGroupMembers();
                setGroupTarget(peds, target);
            }
            else if (getGroupIds().size() > 1) {
                try {
                    throw new Exception("More than 1 groupIds assigned to pedestrian.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        super.setTargets(target);
    }

    public static void setGroupTarget(Collection<Pedestrian> agents, LinkedList<Integer> targetIds) {
        for (Pedestrian a : agents) {
            a.setTargetsOther(targetIds);
        }
    }

    protected void setTargetsOther(LinkedList<Integer> targetIds) {
        super.setTargets(targetIds);
    }


}
