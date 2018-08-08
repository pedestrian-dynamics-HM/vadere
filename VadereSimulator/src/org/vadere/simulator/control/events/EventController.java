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

}
