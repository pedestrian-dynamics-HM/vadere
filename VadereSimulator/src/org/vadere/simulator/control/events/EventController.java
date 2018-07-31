package org.vadere.simulator.control.events;

import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.events.json.EventInfo;
import org.vadere.state.events.types.ElapsedTimeEvent;
import org.vadere.state.events.types.Event;
import org.vadere.state.events.types.EventTimeframe;

import java.util.ArrayList;
import java.util.List;

/**
 * The EventController encapsulates logic to raise events.
 *
 * The EventController uses the passed @see ScenarioStore
 * to extract the possible events from the scenario description.
 */
public class EventController {

    // Variables
    private ScenarioStore scenarioStore;

    // Constructors
    public EventController(ScenarioStore scenarioStore) {
        this.scenarioStore = scenarioStore;
    }

    // Getters
    public ScenarioStore getScenarioStore() {
        return scenarioStore;
    }

    // Setters
    public void setScenarioStore(ScenarioStore scenarioStore) {
        this.scenarioStore = scenarioStore;
    }

    // Methods
    public List<Event> getEventsForTime(double simulationTime) {
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
