package org.vadere.simulator.control.psychology.perception;

import org.jcodec.codecs.mjpeg.JpegDecoder;
import org.lwjgl.system.CallbackI;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.psychology.perception.json.ReactionProbability;
import org.vadere.state.psychology.perception.json.StimulusInfo;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The StimulusController encapsulates logic to inject stimuli
 * into the simulation loop.
 *
 * The StimulusController uses the passed {@link ScenarioStore}
 * to extract the possible stimuli from the scenario description.
 *
 * TODO: Clarify what shall happen if "simTimeSteps" is too coarse
 *   and defined stimuli cannot be triggered correctly here.
 */
public class StimulusController {

    // Variables
    private ScenarioStore scenarioStore;
    private List<StimulusInfo> oneTimeStimuli;
    private List<StimulusInfo> recurringStimuli;
    private HashMap<Integer, Double> reactionProbabilities;

    private HashMap<Pedestrian, List<StimulusInfo>> pedSpecificStimuli;

    // Constructors
    public StimulusController(ScenarioStore scenarioStore) {
        this.scenarioStore = scenarioStore;

        oneTimeStimuli = filterOneTimeStimuli(scenarioStore.getStimulusInfoStore().getStimulusInfos());
        recurringStimuli = filterRecurringStimuli(scenarioStore.getStimulusInfoStore().getStimulusInfos());


        oneTimeStimuli.stream().forEach(stimulusInfo -> throwExceptionIfTimeframeIsInvalid(stimulusInfo.getTimeframe(), false));
        recurringStimuli.stream().forEach(stimulusInfo -> throwExceptionIfTimeframeIsInvalid(stimulusInfo.getTimeframe(), true));

        reactionProbabilities = getReactionProbabilites(scenarioStore.getStimulusInfoStore().getReactionProbabilities());
        pedSpecificStimuli = new HashMap<>();
    }

    private HashMap<Integer, Double> getReactionProbabilites(List<ReactionProbability> reactionProbabilities) {

        reactionProbabilities.forEach(reactionProbability -> throwExceptionIfReactionProbabilityIsInvalid(reactionProbability));
        return (HashMap<Integer, Double>) reactionProbabilities
                .stream()
                .collect(Collectors.toMap(ReactionProbability::getStimulusId,ReactionProbability::getReactionProbability));
    }

    // Getters
    public ScenarioStore getScenarioStore() {
        return scenarioStore;
    }
    public List<StimulusInfo> getOneTimeStimuli() { return oneTimeStimuli; }
    public List<StimulusInfo> getRecurringStimuli() { return recurringStimuli; }

    // Setters
    public void setScenarioStore(ScenarioStore scenarioStore) {
        this.scenarioStore = scenarioStore;
    }
    public void setOneTimeStimuli(List<StimulusInfo> oneTimeStimuli) { this.oneTimeStimuli = oneTimeStimuli; }
    public void setRecurringStimuli(List<StimulusInfo> recurringStimuli) { this.recurringStimuli = recurringStimuli; }

    // Methods
    public List<Stimulus> getStimuliForTime(double simulationTime) {
        List<Stimulus> stimuli = new ArrayList<>();

        // Always, create an "ElapsedTime".
        stimuli.add(new ElapsedTime(simulationTime));

        List<Stimulus> activeOneTimeStimuli = getOneTimeStimuliForSimulationTime(simulationTime);
        List<Stimulus> activeRecurringStimuli = getRecurringStimuliForSimulationTime(simulationTime);

        // Set timestamp for each active stimulus.
        activeOneTimeStimuli.stream().forEach(stimulus -> stimulus.setTime(simulationTime));
        activeRecurringStimuli.stream().forEach((stimulus -> stimulus.setTime(simulationTime)));

        stimuli.addAll(activeOneTimeStimuli);
        stimuli.addAll(activeRecurringStimuli);

        addReactionProbabilityToStimulus(stimuli);

        return stimuli;
    }

    private void addReactionProbabilityToStimulus(final List<Stimulus> stimuli) {
        for (Stimulus stimulus : stimuli){
            if (!(stimulus instanceof ElapsedTime)){
                if (reactionProbabilities.containsKey(stimulus.getId())){
                    stimulus.setPerceptionProbability(reactionProbabilities.get(stimulus.getId()));
                }
                else{
                    throw new RuntimeException("Stimulus id = "+ stimulus.getId() + " is not defined in perceptionLayer/reactionProbabilities." );
                }
            }
        }
    }


    public void setPedSpecificStimuli(final HashMap<Pedestrian, List<StimulusInfo>> pedSpecificStimuli) {
        this.pedSpecificStimuli = pedSpecificStimuli;
    }

    public HashMap<Pedestrian,List<Stimulus>> getStimuliForTime(double simulationTime, Collection<Pedestrian> peds) {

        HashMap<Pedestrian, List<Stimulus>> pedSpecificStimuliForTime = new HashMap<>();

        for (Pedestrian ped : peds) {
            pedSpecificStimuliForTime.put(ped, getStimuliForTime(simulationTime, ped));
        }

        return pedSpecificStimuliForTime;
    }

    public List<Stimulus> getStimuliForTime(double simulationTime, Pedestrian ped) {

        List<Stimulus> stimuli = new ArrayList<>();
        stimuli = getStimuliForTime(simulationTime);

        List<Stimulus> waitInAreaStimuli = stimuli.stream().filter(stimulus -> stimulus instanceof WaitInArea).collect(Collectors.toList());
        List<Stimulus> threatStimuli = stimuli.stream().filter(stimulus -> stimulus instanceof Threat).collect(Collectors.toList());

        Stimulus mostImportantWaitInArea = selectWaitInAreaContainingPedestrian(ped, waitInAreaStimuli);
        Stimulus mostImportantThrea = selectClosestAndPerceptibleThreat(ped,threatStimuli);

        stimuli.removeAll(waitInAreaStimuli);
        stimuli.removeAll(threatStimuli);
        if (mostImportantThrea!= null) stimuli.add(mostImportantThrea);
        if (mostImportantWaitInArea != null)stimuli.add(mostImportantWaitInArea);


        stimuli.addAll(getPedSpecificDynamicStimuli(ped, simulationTime, stimuli));

        return stimuli.stream().distinct().collect(Collectors.toList());

    }

    private List<Stimulus> getPedSpecificDynamicStimuli(Pedestrian pedestrian, double simulationTime, List<Stimulus> stimuli){
        List<Stimulus> activeStimuli = new ArrayList<>();
        if (pedSpecificStimuli.containsKey(pedestrian)){
            pedSpecificStimuli.get(pedestrian).stream()
                    .filter(stimulusInfo -> oneTimeTimeframeIsActiveAtSimulationTime(stimulusInfo.getTimeframe(), simulationTime))
                    .forEach(stimulusInfo -> activeStimuli.addAll(stimulusInfo.getStimuli()));
        }

        List<Stimulus> waitInAreaStimuli = activeStimuli.stream().filter(stimulus -> stimulus instanceof WaitInArea).collect(Collectors.toList());
        List<Stimulus> threatStimuli = activeStimuli.stream().filter(stimulus -> stimulus instanceof Threat).collect(Collectors.toList());

        Stimulus mostImportantWaitInArea = selectWaitInAreaContainingPedestrian(pedestrian, waitInAreaStimuli);
        Stimulus mostImportantThrea = selectClosestAndPerceptibleThreat(pedestrian,threatStimuli);

        activeStimuli.removeAll(waitInAreaStimuli);
        activeStimuli.removeAll(threatStimuli);
        if (mostImportantThrea!= null) activeStimuli.add(mostImportantThrea);
        if (mostImportantWaitInArea != null) activeStimuli.add(mostImportantWaitInArea);

        List<Stimulus> finalstimuli = stimuli;
        List<Stimulus> sorted = activeStimuli.stream().filter(stimulus -> finalstimuli.contains(stimulus) == false).collect(Collectors.toList());

        return sorted;
    }

    private Stimulus selectClosestAndPerceptibleThreat(Pedestrian pedestrian, List<Stimulus> threatStimuli) {
        Threat closestAndPerceptibleThreat = null;
        double distanceToClosestThreat = -1;

        for (Stimulus stimulus : threatStimuli) {
            Threat currentThreat = (Threat) stimulus;

            VPoint threatOrigin = this.scenarioStore.getTopography().getTarget(currentThreat.getOriginAsTargetId()).getShape().getCentroid();
            double distanceToThreat = threatOrigin.distance(pedestrian.getPosition());

            if (distanceToThreat <= currentThreat.getRadius()) {
                if (closestAndPerceptibleThreat == null) {
                    closestAndPerceptibleThreat = currentThreat;
                    distanceToClosestThreat = distanceToThreat;
                } else {
                    if (distanceToThreat < distanceToClosestThreat) {
                        closestAndPerceptibleThreat = currentThreat;
                        distanceToClosestThreat = distanceToThreat;
                    }
                }
            }
        }

        return closestAndPerceptibleThreat;
    }

    private Stimulus selectWaitInAreaContainingPedestrian(Pedestrian pedestrian, List<Stimulus> waitInAreaStimuli) {
        WaitInArea selectedWaitInArea = null;

        for (Stimulus stimulus : waitInAreaStimuli) {
            WaitInArea waitInArea = (WaitInArea) stimulus;
            boolean pedInArea = waitInArea.getArea().contains(pedestrian.getPosition());

            if (pedInArea) {
                selectedWaitInArea = waitInArea;
            }
        }

        return selectedWaitInArea;
    }


    public void setDynamicStimulus(StimulusInfo stimulusInfo){
        List<StimulusInfo> stimuliList = new ArrayList<>();
        stimuliList.add(stimulusInfo);

        List<StimulusInfo> oneTimeDynamicStimuli = filterOneTimeStimuli(stimuliList);
        List<StimulusInfo> recurringDynamicStimuli = filterRecurringStimuli(stimuliList);

        oneTimeStimuli.addAll(oneTimeDynamicStimuli);
        recurringStimuli.addAll(recurringDynamicStimuli);
    }


    private void setDynamicStimulus(Collection<Pedestrian> peds, StimulusInfo stimulusInfo){
        for (Pedestrian ped : peds){
            setDynamicStimulus(ped, stimulusInfo);
        }
    }

    public void setDynamicStimulus(Pedestrian ped, StimulusInfo stimulusInfo){
        //TODO: check whether if-else is necessary

        if (pedSpecificStimuli.containsKey(ped)) {
            pedSpecificStimuli.get(ped).add(stimulusInfo);
        }
        else{
            List<StimulusInfo> stimulusInfos = new ArrayList<>();
            stimulusInfos.add(stimulusInfo);
            pedSpecificStimuli.put(ped, stimulusInfos);
        }
    }

    public void setDynamicStimulus(Pedestrian ped, Stimulus stimulus, double simTimeNextTimeStep){
        List<StimulusInfo> stimuliList = new ArrayList<>();
        if (pedSpecificStimuli.containsKey(ped)){
            stimuliList.addAll(pedSpecificStimuli.get(ped));
        }

        List<Stimulus> newStimulus = new ArrayList<>();
        newStimulus.add(stimulus);
        addReactionProbabilityToStimulus(newStimulus);
        StimulusInfo stimulusInfo = new StimulusInfo();
        stimulusInfo.setTimeframe(new Timeframe(0, simTimeNextTimeStep, false, 0));
        stimulusInfo.setStimuli(newStimulus);

        stimuliList.add(stimulusInfo);

        pedSpecificStimuli.put(ped, stimuliList);

    }


    private List<Stimulus> getOneTimeStimuliForSimulationTime(double simulationTime) {
        List<Stimulus> activeStimuli = new ArrayList<>();

        oneTimeStimuli.stream()
                .filter(stimulusInfo -> oneTimeTimeframeIsActiveAtSimulationTime(stimulusInfo.getTimeframe(), simulationTime))
                .forEach(stimulusInfo -> activeStimuli.addAll(stimulusInfo.getStimuli()));

        return activeStimuli;
    }

    private List<Stimulus> getRecurringStimuliForSimulationTime(double simulationTime) {
        List<Stimulus> activeStimuli = new ArrayList<>();

        recurringStimuli.stream()
                .filter(stimulusInfo -> timeframeIsActiveAtSimulationTime(stimulusInfo.getTimeframe(), simulationTime))
                .forEach(stimulusInfo -> activeStimuli.addAll(stimulusInfo.getStimuli()));

        return activeStimuli;
    }

    public static List<StimulusInfo> filterOneTimeStimuli(List<StimulusInfo> lsi){
        return lsi.stream()
                .filter(stimulusInfo -> stimulusInfo.getTimeframe().isRepeat() == false)
                .collect(Collectors.toList());
    }

    public static List<StimulusInfo> filterRecurringStimuli(List<StimulusInfo> lsi){
        return lsi.stream()
                .filter(stimulusInfo -> stimulusInfo.getTimeframe().isRepeat() == true)
                .collect(Collectors.toList());
    }

    /**
     * Return if "simulationTime" is in interval [startTime, endTime] of given "timeframe".
     *
     * @throws IllegalArgumentException If given timeframe is a recurring one.
     */
    public static boolean oneTimeTimeframeIsActiveAtSimulationTime(Timeframe timeframe, double simulationTime) {
        throwExceptionIfTimeframeIsInvalid(timeframe, false);

        boolean stimuliIsActive = (simulationTime >= timeframe.getStartTime() && simulationTime <= timeframe.getEndTime());

        return stimuliIsActive;
    }

    /**
     * Given a (recurring) "timeframe" and a "simulationTime" return if the
     * timeframe is "active" at that specific "simulationTime" or not.
     *
     * An {@link Timeframe} contains "startTime", "endTime" and"waitTimeBetweenRepetition" for
     * an {@link Stimulus}. With "startTime", "endTime" and "waitTimeBetweenRepetition" you can calculate
     * the period length of an event:
     *
     *   period_length = (endTime - startTime) + waitTimeBetweenRepetition
     *
     * With this information, you can define intervals in which the timeframe is active. For instance:
     *
     *   startTime = 0.75
     *   endTime = 1.25
     *   waitTimeBetweenRepetition = 1.0
     *   simulationTime = 0.8
     *
     * That means, the timeframe is active in following intervals (end time is included!):
     *
     *   [0.75 -- 1.25]
     *   [2.25 -- 2.75]
     *   [3.75 -- 4.25]
     *   [5.25 -- 5.75]
     *   ...
     *
     * Now, if this method gets "simulationTime = 0.8", the method should detect that the timeframe
     * is active in that given time.
     *
     * @throws IllegalArgumentException If given timeframe is a one-time timeframe.
     */
    public static boolean timeframeIsActiveAtSimulationTime(Timeframe timeframe, double simulationTime) {
        throwExceptionIfTimeframeIsInvalid(timeframe, true);

        double stimulusLength = timeframe.getEndTime() - timeframe.getStartTime();
        double stimulusPeriodLength = stimulusLength + timeframe.getWaitTimeBetweenRepetition();

        double normalizedSimulationTime = Math.max(0, (simulationTime - timeframe.getStartTime()));
        // Check with unit testing if cut-off is okay here or if we need rounding.
        int currentPeriod = (int)(normalizedSimulationTime / stimulusPeriodLength);

        double stimulusStartTimeCurrentPeriod = timeframe.getStartTime() + (currentPeriod * stimulusPeriodLength);
        double stimulusEndTimeCurrentPeriod = stimulusStartTimeCurrentPeriod + stimulusLength;

        boolean stimulusIsActive = (simulationTime >= stimulusStartTimeCurrentPeriod && simulationTime <= stimulusEndTimeCurrentPeriod);

        return stimulusIsActive;
    }

    /**
     * Throw {@link IllegalArgumentException} if startTime > endTime OR timeframe does not meet recurring expectation.
     */
    private static void throwExceptionIfTimeframeIsInvalid(Timeframe timeframe, boolean expectRecurring) {
        if (timeframe.getStartTime() > timeframe.getEndTime()) {
            throw new IllegalArgumentException("Timeframe: startTime > endTime!");
        }

        if (timeframe.isRepeat() != expectRecurring) {
            throw new IllegalArgumentException(String.format("Timeframe: \"repeat=%b\" expected!", expectRecurring));
        }
    }

    /**
     * Throw {@link IllegalArgumentException} if probability is not in 0..1
     */
    private static void throwExceptionIfReactionProbabilityIsInvalid(ReactionProbability reactionProbability) {
        double probability = reactionProbability.getReactionProbability();
        if ( probability > 1.0 || probability < 0.0) {
            throw new IllegalArgumentException("ReactionProbability: probability must be in [0,1]. Got " + probability);
        }
    }


}
