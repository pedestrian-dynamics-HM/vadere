package org.vadere.simulator.control.events;

import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.events.json.EventInfo;
import org.vadere.state.events.types.ElapsedTimeEvent;
import org.vadere.state.events.types.Event;
import org.vadere.state.events.types.EventTimeframe;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The EventController encapsulates logic to raise events.
 *
 * The EventController uses the passed {@link ScenarioStore}
 * to extract the possible events from the scenario description.
 *
 * TODO: Clarify what should happen if "simTimeSteps" is too coarse
 * and defined events cannot be triggered correctly here.
 */
public class EventController {

    // Variables
    private ScenarioStore scenarioStore;
    private List<EventInfo> oneTimeEvents;
    private List<EventInfo> recurringEvents;

    // Constructors
    public EventController(ScenarioStore scenarioStore) {
        this.scenarioStore = scenarioStore;

        oneTimeEvents = scenarioStore.getEventInfoStore().getEventInfos().stream()
                .filter(eventInfo -> eventInfo.getEventTimeframe().isRepeat() == false)
                .collect(Collectors.toList());

        recurringEvents = scenarioStore.getEventInfoStore().getEventInfos().stream()
                .filter(eventInfo -> eventInfo.getEventTimeframe().isRepeat() == true)
                .collect(Collectors.toList());

        oneTimeEvents.stream().forEach(eventInfo -> throwExceptionIfTimeframeIsInvalid(eventInfo.getEventTimeframe(), false));
        recurringEvents.stream().forEach(eventInfo -> throwExceptionIfTimeframeIsInvalid(eventInfo.getEventTimeframe(), true));
    }

    // Getters
    public ScenarioStore getScenarioStore() {
        return scenarioStore;
    }
    public List<EventInfo> getOneTimeEvents() { return oneTimeEvents; }
    public List<EventInfo> getRecurringEvents() { return recurringEvents; }

    // Setters
    public void setScenarioStore(ScenarioStore scenarioStore) {
        this.scenarioStore = scenarioStore;
    }
    public void setOneTimeEvents(List<EventInfo> oneTimeEvents) { this.oneTimeEvents = oneTimeEvents; }
    public void setRecurringEvents(List<EventInfo> recurringEvents) { this.recurringEvents = recurringEvents; }

    // Methods
    public List<Event> getEventsForTime(double simulationTime) {
        List<Event> events = new ArrayList<>();

        // Always, create an "ElapsedTimeEvent".
        events.add(new ElapsedTimeEvent(simulationTime));

        List<Event> activeOneTimeEvents = getOneTimeEventsForSimulationTime(simulationTime);
        List<Event> activeRecurringEvents = getRecurringEventsForSimulationTime(simulationTime);

        // Set timestamp for each active event.
        activeOneTimeEvents.stream().forEach(event -> event.setTime(simulationTime));
        activeRecurringEvents.stream().forEach((event -> event.setTime(simulationTime)));

        events.addAll(activeOneTimeEvents);
        events.addAll(activeRecurringEvents);

        return events;
    }

    private List<Event> getOneTimeEventsForSimulationTime(double simulationTime) {
        List<Event> activeEvents = new ArrayList<>();

        oneTimeEvents.stream()
                .filter(eventInfo -> oneTimeTimeframeIsActiveAtSimulationTime(eventInfo.getEventTimeframe(), simulationTime))
                .forEach(eventInfo -> activeEvents.addAll(eventInfo.getEvents()));

        return activeEvents;
    }

    private List<Event> getRecurringEventsForSimulationTime(double simulationTime) {
        List<Event> activeEvents = new ArrayList<>();

        recurringEvents.stream()
                .filter(eventInfo -> timeframeIsActiveAtSimulationTime(eventInfo.getEventTimeframe(), simulationTime))
                .forEach(eventInfo -> activeEvents.addAll(eventInfo.getEvents()));

        return activeEvents;
    }

    /**
     * Return if "simulationTime" is in interval [startTime, endTime] of given "timeframe".
     *
     * @throws IllegalArgumentException If given timeframe is a recurring one.
     */
    public static boolean oneTimeTimeframeIsActiveAtSimulationTime(EventTimeframe timeframe, double simulationTime) {
        throwExceptionIfTimeframeIsInvalid(timeframe, false);

        boolean eventIsActive = (simulationTime >= timeframe.getStartTime() && simulationTime <= timeframe.getEndTime());

        return eventIsActive;
    }

    /**
     * Given a (recurring) "timeframe" and a "simulationTime" return if the
     * timeframe is "active" at that specific "simulationTime" or not.
     *
     * An {@link EventTimeframe} contains "startTime", "endTime" and"waitTimeBetweenRepetition" for
     * an {@link Event}. With "startTime", "endTime" and "waitTimeBetweenRepetition" you can calculate
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
    public static boolean timeframeIsActiveAtSimulationTime(EventTimeframe timeframe, double simulationTime) {
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
    private static void throwExceptionIfTimeframeIsInvalid(EventTimeframe timeframe, boolean expectRecurring) {
        if (timeframe.getStartTime() > timeframe.getEndTime()) {
            throw new IllegalArgumentException("EventTimeframe: startTime > endTime!");
        }

        if (timeframe.isRepeat() != expectRecurring) {
            throw new IllegalArgumentException(String.format("EventTimeframe: \"repeat=%b\" expected!", expectRecurring));
        }
    }

}
