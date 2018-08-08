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
 * The EventController uses the passed @see ScenarioStore
 * to extract the possible events from the scenario description.
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
        // TODO Handle one-time and recurring events properly.
        List<Event> events = new ArrayList<>();

        // Always, create an "ElapsedTimeEvent".
        events.add(new ElapsedTimeEvent(simulationTime));

        for (EventInfo eventInfo : scenarioStore.getEventInfoStore().getEventInfos()) {
            EventTimeframe eventTimeframe = eventInfo.getEventTimeframe();

            if (simulationTime >= eventTimeframe.getStartTime() && simulationTime <= eventTimeframe.getEndTime()) {
                events.addAll(eventInfo.getEvents());
            }
        }

        return events;
    }

    /**
     * Given a "simulationTime", return all events which must be raised at that point in time
     * (according to their timeframe description).
     *
     * An @see EventTimeframe contains "startTime", "endTime" and "waitTimeBetweenRepetition" for
     * an @see Event. With "startTime", "endTime" and "waitTimeBetweenRepetition" you can calculate
     * the frequency of an event:
     *
     *   frequency = (endTime - startTime) * waitTimeBetweenRepetition
     *
     * With this information, you can define timeframes in which the event is active. For instance:
     *
     *   startTime = 0.75
     *   endTime = 1.25
     *   waitTimeBetweenRepetition = 1.0
     *   simulationTime = 0.8
     *
     * That means, the event is active in following timeframes (end time should be excluded):
     *
     *   [0.75 -- 1.25)
     *   [2.25 -- 2.75)
     *   [3.75 -- 4.25)
     *   [5.25 -- 5.75)
     *   ...
     *
     * Now, if this method gets "simulationTime = 0.8", the method should detect that the event
     * is active during this time.
     *
     * // TODO Write unit tests for this method.
     */
    public List<Event> getRecurringEventsForSimulationTime(double simulationTime) {
        List<Event> activeEvents = new ArrayList<>();

        for (EventInfo eventInfo : recurringEvents) {
            EventTimeframe timeframe = eventInfo.getEventTimeframe();

            double eventLength = timeframe.getEndTime() - timeframe.getStartTime();
            double eventFrequency = eventLength + timeframe.getWaitTimeBetweenRepetition();

            // TODO Check if implementation is correct or "if (simulationTime < startTime)" must be introduced.
            double normalizedSimulationTime = Math.max(0, Math.floor(simulationTime) - 1);
            double startTimeForNextEvent = timeframe.getStartTime() + (normalizedSimulationTime * eventFrequency);
            double endTimeForNextEvent = startTimeForNextEvent + eventLength;

            boolean eventIsActive = (simulationTime >= startTimeForNextEvent && simulationTime < endTimeForNextEvent);

            if (eventIsActive) {
                activeEvents.addAll(eventInfo.getEvents());
            }
        }

        return activeEvents;
    }

}
