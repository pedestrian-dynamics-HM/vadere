package org.vadere.simulator.control.events;

import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.psychology.stimuli.json.StimulusInfo;
import org.vadere.state.psychology.stimuli.types.ElapsedTime;
import org.vadere.state.psychology.stimuli.types.Stimulus;
import org.vadere.state.psychology.stimuli.types.Timeframe;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The EventController encapsulates logic to raise stimuli.
 *
 * The EventController uses the passed {@link ScenarioStore}
 * to extract the possible stimuli from the scenario description.
 *
 * TODO: Clarify what should happen if "simTimeSteps" is too coarse
 * and defined stimuli cannot be triggered correctly here.
 */
public class EventController {

    // Variables
    private ScenarioStore scenarioStore;
    private List<StimulusInfo> oneTimeEvents;
    private List<StimulusInfo> recurringEvents;

    // Constructors
    public EventController(ScenarioStore scenarioStore) {
        this.scenarioStore = scenarioStore;

        oneTimeEvents = scenarioStore.getStimulusInfoStore().getStimulusInfos().stream()
                .filter(eventInfo -> eventInfo.getTimeframe().isRepeat() == false)
                .collect(Collectors.toList());

        recurringEvents = scenarioStore.getStimulusInfoStore().getStimulusInfos().stream()
                .filter(eventInfo -> eventInfo.getTimeframe().isRepeat() == true)
                .collect(Collectors.toList());

        oneTimeEvents.stream().forEach(eventInfo -> throwExceptionIfTimeframeIsInvalid(eventInfo.getTimeframe(), false));
        recurringEvents.stream().forEach(eventInfo -> throwExceptionIfTimeframeIsInvalid(eventInfo.getTimeframe(), true));
    }

    // Getters
    public ScenarioStore getScenarioStore() {
        return scenarioStore;
    }
    public List<StimulusInfo> getOneTimeEvents() { return oneTimeEvents; }
    public List<StimulusInfo> getRecurringEvents() { return recurringEvents; }

    // Setters
    public void setScenarioStore(ScenarioStore scenarioStore) {
        this.scenarioStore = scenarioStore;
    }
    public void setOneTimeEvents(List<StimulusInfo> oneTimeEvents) { this.oneTimeEvents = oneTimeEvents; }
    public void setRecurringEvents(List<StimulusInfo> recurringEvents) { this.recurringEvents = recurringEvents; }

    // Methods
    public List<Stimulus> getEventsForTime(double simulationTime) {
        List<Stimulus> stimuli = new ArrayList<>();

        // Always, create an "ElapsedTime".
        stimuli.add(new ElapsedTime(simulationTime));

        List<Stimulus> activeOneTimeStimuli = getOneTimeEventsForSimulationTime(simulationTime);
        List<Stimulus> activeRecurringStimuli = getRecurringEventsForSimulationTime(simulationTime);

        // Set timestamp for each active event.
        activeOneTimeStimuli.stream().forEach(event -> event.setTime(simulationTime));
        activeRecurringStimuli.stream().forEach((event -> event.setTime(simulationTime)));

        stimuli.addAll(activeOneTimeStimuli);
        stimuli.addAll(activeRecurringStimuli);

        return stimuli;
    }

    private List<Stimulus> getOneTimeEventsForSimulationTime(double simulationTime) {
        List<Stimulus> activeStimuli = new ArrayList<>();

        oneTimeEvents.stream()
                .filter(eventInfo -> oneTimeTimeframeIsActiveAtSimulationTime(eventInfo.getTimeframe(), simulationTime))
                .forEach(eventInfo -> activeStimuli.addAll(eventInfo.getStimuli()));

        return activeStimuli;
    }

    private List<Stimulus> getRecurringEventsForSimulationTime(double simulationTime) {
        List<Stimulus> activeStimuli = new ArrayList<>();

        recurringEvents.stream()
                .filter(eventInfo -> timeframeIsActiveAtSimulationTime(eventInfo.getTimeframe(), simulationTime))
                .forEach(eventInfo -> activeStimuli.addAll(eventInfo.getStimuli()));

        return activeStimuli;
    }

    /**
     * Return if "simulationTime" is in interval [startTime, endTime] of given "timeframe".
     *
     * @throws IllegalArgumentException If given timeframe is a recurring one.
     */
    public static boolean oneTimeTimeframeIsActiveAtSimulationTime(Timeframe timeframe, double simulationTime) {
        throwExceptionIfTimeframeIsInvalid(timeframe, false);

        boolean eventIsActive = (simulationTime >= timeframe.getStartTime() && simulationTime <= timeframe.getEndTime());

        return eventIsActive;
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

        double eventLength = timeframe.getEndTime() - timeframe.getStartTime();
        double eventPeriodLength = eventLength + timeframe.getWaitTimeBetweenRepetition();

        double normalizedSimulationTime = Math.max(0, (simulationTime - timeframe.getStartTime()));
        // Check with unit testing if cut-off is okay here or if we need rounding.
        int currentPeriod = (int)(normalizedSimulationTime / eventPeriodLength);

        double eventStartTimeCurrentPeriod = timeframe.getStartTime() + (currentPeriod * eventPeriodLength);
        double eventEndTimeCurrentPeriod = eventStartTimeCurrentPeriod + eventLength;

        boolean eventIsActive = (simulationTime >= eventStartTimeCurrentPeriod && simulationTime <= eventEndTimeCurrentPeriod);

        return eventIsActive;
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

}
