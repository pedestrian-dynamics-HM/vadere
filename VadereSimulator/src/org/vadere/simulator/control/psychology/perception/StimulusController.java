package org.vadere.simulator.control.psychology.perception;

import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.psychology.perception.json.StimulusInfo;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The StimulusController encapsulates logic to inject stimuli
 * into the simulation loop.
 * <p>
 * The StimulusController uses the passed {@link ScenarioStore}
 * to extract the possible stimuli from the scenario description.
 * <p>
 * TODO: Clarify what shall happen if "simTimeSteps" is too coarse
 *   and defined stimuli cannot be triggered correctly here.
 */
public class StimulusController {

    // Variables
    private ScenarioStore scenarioStore;
    private List<StimulusInfo> oneTimeStimuli;
    private List<StimulusInfo> recurringStimuli;


    // Constructors
    public StimulusController(ScenarioStore scenarioStore) {
        this.scenarioStore = scenarioStore;
        setCheckStimuli();
    }

    // Getters
    public ScenarioStore getScenarioStore() {
        return scenarioStore;
    }

    public List<StimulusInfo> getOneTimeStimuli() {
        return oneTimeStimuli;
    }

    public List<StimulusInfo> getRecurringStimuli() {
        return recurringStimuli;
    }


    // Setters
    public void setScenarioStore(ScenarioStore scenarioStore) {
        this.scenarioStore = scenarioStore;
    }

    public void setOneTimeStimuli(List<StimulusInfo> oneTimeStimuli) {
        this.oneTimeStimuli = oneTimeStimuli;
    }

    public void setRecurringStimuli(List<StimulusInfo> recurringStimuli) {
        this.recurringStimuli = recurringStimuli;
    }


    // Methods

    public List<Stimulus> getStimuliFilteredTimeOnly(double simulationTime) {
        return getStimuliFiltered(simulationTime, null, null);
    }


    public List<Stimulus> getStimuliFiltered(double simulationTime, VPoint position, Integer pedId) {

        setCheckStimuli();

        List<Stimulus> stimuli = new ArrayList<>();

        // Always, create an "ElapsedTime".
        stimuli.add(new ElapsedTime(simulationTime));

        List<Stimulus> activeOneTimeStimuli = getOneTimeStimuliForSimulationTime(simulationTime, position, pedId);
        List<Stimulus> activeRecurringStimuli = getRecurringStimuliForSimulationTime(simulationTime, position, pedId);

        // Set timestamp for each active stimulus.
        activeOneTimeStimuli.stream().forEach(stimulus -> stimulus.setTime(simulationTime));
        activeRecurringStimuli.stream().forEach((stimulus -> stimulus.setTime(simulationTime)));

        stimuli.addAll(activeOneTimeStimuli);
        stimuli.addAll(activeRecurringStimuli);

        return stimuli;
    }

    public HashMap<Pedestrian, List<Stimulus>> getStimuli(double simulationTime, Collection<Pedestrian> peds) {

        HashMap<Pedestrian, List<Stimulus>> pedSpecificStimuliForTime = new HashMap<>();
        for (Pedestrian ped : peds) {
            pedSpecificStimuliForTime.put(ped, getStimuliFiltered(simulationTime, ped.getPosition(), ped.getId()));
        }

        return pedSpecificStimuliForTime;
    }

    private void setCheckStimuli() {
        oneTimeStimuli = filterOneTimeStimuli(scenarioStore.getStimulusInfoStore().getStimulusInfos());
        recurringStimuli = filterRecurringStimuli(scenarioStore.getStimulusInfoStore().getStimulusInfos());
        oneTimeStimuli.stream().forEach(stimulusInfo -> throwExceptionIfTimeframeIsInvalid(stimulusInfo.getTimeframe(), false));
        recurringStimuli.stream().forEach(stimulusInfo -> throwExceptionIfTimeframeIsInvalid(stimulusInfo.getTimeframe(), true));
    }


   private List<Stimulus> getOneTimeStimuliForSimulationTime(double simulationTime, VPoint position, Integer pedId) {
       List<Stimulus> activeStimuli = new ArrayList<>();

       oneTimeStimuli.stream().filter(stimulusInfo ->
                       oneTimeTimeframeIsActiveAtSimulationTime(stimulusInfo.getTimeframe(), simulationTime)
                               && pedIsInSpecifiedArea(position, stimulusInfo.getLocation())
                               && pedIsAffected(pedId, stimulusInfo.getSubpopulationFilter())
               )
               .forEach(stimulusInfo -> activeStimuli.addAll(stimulusInfo.getStimuli()));


       return activeStimuli;

   }




    private List<Stimulus> getRecurringStimuliForSimulationTime(double simulationTime, VPoint position, Integer pedId) {
        List<Stimulus> activeStimuli = new ArrayList<>();

        recurringStimuli.stream()
                .filter(stimulusInfo ->
                        timeframeIsActiveAtSimulationTime(stimulusInfo.getTimeframe(), simulationTime)
                                && pedIsInSpecifiedArea(position, stimulusInfo.getLocation())
                                && pedIsAffected(pedId, stimulusInfo.getSubpopulationFilter())
                )
                .forEach(stimulusInfo -> activeStimuli.addAll(stimulusInfo.getStimuli()));

        return activeStimuli;
    }


    public static List<StimulusInfo> filterOneTimeStimuli(List<StimulusInfo> lsi) {
        return lsi.stream()
                .filter(stimulusInfo -> stimulusInfo.getTimeframe().isRepeat() == false)
                .collect(Collectors.toList());
    }

    public static List<StimulusInfo> filterRecurringStimuli(List<StimulusInfo> lsi) {
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
     * <p>
     * An {@link Timeframe} contains "startTime", "endTime" and"waitTimeBetweenRepetition" for
     * an {@link Stimulus}. With "startTime", "endTime" and "waitTimeBetweenRepetition" you can calculate
     * the period length of an event:
     * <p>
     * period_length = (endTime - startTime) + waitTimeBetweenRepetition
     * <p>
     * With this information, you can define intervals in which the timeframe is active. For instance:
     * <p>
     * startTime = 0.75
     * endTime = 1.25
     * waitTimeBetweenRepetition = 1.0
     * simulationTime = 0.8
     * <p>
     * That means, the timeframe is active in following intervals (end time is included!):
     * <p>
     * [0.75 -- 1.25]
     * [2.25 -- 2.75]
     * [3.75 -- 4.25]
     * [5.25 -- 5.75]
     * ...
     * <p>
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
        int currentPeriod = (int) (normalizedSimulationTime / stimulusPeriodLength);

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

    private boolean pedIsAffected(Integer pedId, SubpopulationFilter filter) {

        boolean stimulusIsActive = true;

        List<Integer> ids = filter.getAffectedPedestrianIds();
        if (ids.size() > 0 && pedId != null) {
            stimulusIsActive = ids.contains(pedId);
        }
        // if there is no ped id defined, I assume that the StimulusInfo is valid for any agent
        return stimulusIsActive;
    }

    private boolean pedIsInSpecifiedArea(VPoint position, Location location) {

        boolean stimulusIsActive = true;

        List<VShape> areas = location.getAreas();
        if (areas.size() > 0 && position != null) {
            stimulusIsActive = location.getAreas().stream().anyMatch(area -> area.contains(position));
        }
        // if there is no area defined, I assume that the StimulusInfo is valid for the whole topography
        return stimulusIsActive;
    }


}
