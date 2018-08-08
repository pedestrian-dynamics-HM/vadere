package org.vadere.simulator.control.events;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.events.json.EventInfo;
import org.vadere.state.events.json.EventInfoStore;
import org.vadere.state.events.types.Event;
import org.vadere.state.events.types.EventTimeframe;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class EventControllerTest {

    private EventInfoStore getEventInfoStoreContainingRecurringEvent(boolean recurringEvent) {
        // Create "EventTimeframe" and "Event" objects and encapsulate them in "EventInfo" objects.
        EventTimeframe eventTimeframe = new EventTimeframe(5, 30, recurringEvent, 0);
        List<Event> events = new ArrayList<>();

        EventInfo eventInfo1 = new EventInfo();
        eventInfo1.setEventTimeframe(eventTimeframe);
        eventInfo1.setEvents(events);

        List<EventInfo> eventInfos = new ArrayList<>();
        eventInfos.add(eventInfo1);

        EventInfoStore eventInfoStore = new EventInfoStore();
        eventInfoStore.setEventInfos(eventInfos);

        return eventInfoStore;
    }

    @Test
    public void EventControllerConstructorFindsNoEventsIfPassingEmptyScenarioStore() {
        ScenarioStore emptyScenarioStore = new ScenarioStore("emptyScenarioStore");

        EventController eventController = new EventController(emptyScenarioStore);

        int expectedEvents = 0;

        assertEquals(expectedEvents, eventController.getOneTimeEvents().size());
        assertEquals(expectedEvents, eventController.getRecurringEvents().size());
    }

    @Test
    public void EventControllerConstructorDetectsOneTimeEventsProperly() {
        ScenarioStore scenarioStoreContainingOneOneTimeEvent = new ScenarioStore("name",
                "description",
                "mainModel",
                null,
                null,
                null,
                getEventInfoStoreContainingRecurringEvent(false));

        EventController eventController = new EventController(scenarioStoreContainingOneOneTimeEvent);

        assertEquals(1, eventController.getOneTimeEvents().size());
        assertEquals(0, eventController.getRecurringEvents().size());
    }

    @Test
    public void EventControllerConstructorDetectsRecurringEventsProperly() {
        ScenarioStore scenarioStoreContainingOneRecurringEvent = new ScenarioStore("name",
                "description",
                "mainModel",
                null,
                null,
                null,
                getEventInfoStoreContainingRecurringEvent(true));

        EventController eventController = new EventController(scenarioStoreContainingOneRecurringEvent);

        assertEquals(0, eventController.getOneTimeEvents().size());
        assertEquals(1, eventController.getRecurringEvents().size());
    }

    @Test
    public void EventControllerConstructorDetectsOneTimeAndRecurringEventsProperly() {
        List<EventInfo> oneTimeAndRecurringEvents = new ArrayList<>();
        oneTimeAndRecurringEvents.addAll(getEventInfoStoreContainingRecurringEvent(false).getEventInfos());
        oneTimeAndRecurringEvents.addAll(getEventInfoStoreContainingRecurringEvent(true).getEventInfos());

        EventInfoStore eventInfoStoreWithBothEvents = new EventInfoStore();
        eventInfoStoreWithBothEvents.setEventInfos(oneTimeAndRecurringEvents);

        ScenarioStore scenarioStoreContainingOneTimeAndRecurringEvent = new ScenarioStore("name",
                "description",
                "mainModel",
                null,
                null,
                null,
                eventInfoStoreWithBothEvents);

        EventController eventController = new EventController(scenarioStoreContainingOneTimeAndRecurringEvent);

        assertEquals(1, eventController.getOneTimeEvents().size());
        assertEquals(1, eventController.getRecurringEvents().size());
    }

}