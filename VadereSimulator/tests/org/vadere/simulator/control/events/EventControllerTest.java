package org.vadere.simulator.control.events;

import org.junit.Test;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.events.json.EventInfo;
import org.vadere.state.events.json.EventInfoStore;
import org.vadere.state.events.types.ElapsedTimeEvent;
import org.vadere.state.events.types.Event;
import org.vadere.state.events.types.EventTimeframe;
import org.vadere.state.events.types.WaitEvent;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class EventControllerTest {

    private ScenarioStore getScenarioStoreContainingRecurringEvent(boolean isRecurring) {
        return new ScenarioStore("name",
                "description",
                "mainModel",
                null,
                null,
                null,
                getEventInfoStoreContainingRecurringEvent(isRecurring));
    }

    private EventInfoStore getEventInfoStoreContainingRecurringEvent(boolean isRecurring) {
        // Create "EventTimeframe" and "Event" objects and encapsulate them in "EventInfo" objects.
        EventTimeframe eventTimeframe = new EventTimeframe(5, 30, isRecurring, 0);
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
    public void eventControllerConstructorFindsNoEventsIfPassingEmptyScenarioStore() {
        ScenarioStore emptyScenarioStore = new ScenarioStore("emptyScenarioStore");

        EventController eventController = new EventController(emptyScenarioStore);

        assertEquals(0, eventController.getOneTimeEvents().size());
        assertEquals(0, eventController.getRecurringEvents().size());
    }

    @Test
    public void eventControllerConstructorDetectsOneTimeEventsProperly() {
        boolean isRecurringEvent = false;
        ScenarioStore scenarioStoreContainingOneOneTimeEvent = getScenarioStoreContainingRecurringEvent(isRecurringEvent);

        EventController eventController = new EventController(scenarioStoreContainingOneOneTimeEvent);

        assertEquals(1, eventController.getOneTimeEvents().size());
        assertEquals(0, eventController.getRecurringEvents().size());
    }

    @Test
    public void eventControllerConstructorDetectsRecurringEventsProperly() {
        boolean isRecurringEvent = true;
        ScenarioStore scenarioStoreContainingOneRecurringEvent = getScenarioStoreContainingRecurringEvent(isRecurringEvent);

        EventController eventController = new EventController(scenarioStoreContainingOneRecurringEvent);

        assertEquals(0, eventController.getOneTimeEvents().size());
        assertEquals(1, eventController.getRecurringEvents().size());
    }

    @Test
    public void eventControllerConstructorDetectsOneTimeAndRecurringEventsProperly() {
        // Create a list containing one one-time and one recurring event.
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

    @Test(expected = IllegalArgumentException.class)
    public void timeframeIsActiveAtSimulationTimeThrowsExceptionIfNoRecurringEventTimeframe() {
        boolean isRecurringEvent = false;
        double simulationTime = 0.8;

        EventTimeframe timeframe = new EventTimeframe(0.75, 1.25, isRecurringEvent, 1.0);

        EventController.timeframeIsActiveAtSimulationTime(timeframe, simulationTime);
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsTrueIfSimulationTimeFallsOntoStartTimePeriodically() {
        boolean recurringEvent = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 1.0;
        double simulationTime = startTime;
        double periodicity = (endTime - startTime) + waitTimeBetweenRepetition;

        EventTimeframe timeframe = new EventTimeframe(startTime, endTime, recurringEvent, waitTimeBetweenRepetition);

        for (int i = 0; i < 50; i++) {
            double currentSimulationTime = simulationTime + (i * periodicity);

            boolean timeframeIsActive = EventController.timeframeIsActiveAtSimulationTime(timeframe, currentSimulationTime);
            assertTrue(timeframeIsActive);
        }
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsFalseIfSimulationTimeFallsOntoEndTimePeriodically() {
        boolean recurringEvent = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 1.0;
        double simulationTime = endTime;
        double periodicity = (endTime - startTime) + waitTimeBetweenRepetition;

        EventTimeframe timeframe = new EventTimeframe(startTime, endTime, recurringEvent, waitTimeBetweenRepetition);

        for (int i = 0; i < 50; i++) {
            double currentSimulationTime = simulationTime + (i * periodicity);

            boolean timeframeIsActive = EventController.timeframeIsActiveAtSimulationTime(timeframe, currentSimulationTime);
            assertFalse(timeframeIsActive);
        }
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsTrueIfSimulationTimeFallsBetweenStartAndEndTimePeriodically() {
        boolean recurringEvent = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 1.0;
        double simulationTime = 1.0;
        double periodicity = (endTime - startTime) + waitTimeBetweenRepetition;

        EventTimeframe timeframe = new EventTimeframe(startTime, endTime, recurringEvent, waitTimeBetweenRepetition);

        for (int i = 0; i < 50; i++) {
            double currentSimulationTime = simulationTime + (i * periodicity);

            boolean timeframeIsActive = EventController.timeframeIsActiveAtSimulationTime(timeframe, currentSimulationTime);
            assertTrue(timeframeIsActive);
        }
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsFalseIfSimulationTimeIsBeforeStartTime() {
        boolean recurringEvent = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 1.0;
        double simulationTime = 0.10;
        double increment = 0.05;

        EventTimeframe timeframe = new EventTimeframe(startTime, endTime, recurringEvent, waitTimeBetweenRepetition);

        while (simulationTime < startTime) {
            boolean timeframeIsActive = EventController.timeframeIsActiveAtSimulationTime(timeframe, simulationTime);
            assertFalse(timeframeIsActive);

            simulationTime += increment;
        }
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsTrueIfSimulationTimeBetweenStartAndEndTime() {
        boolean recurringEvent = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 1.0;
        double simulationTime = startTime;
        double increment = 0.05;

        EventTimeframe timeframe = new EventTimeframe(startTime, endTime, recurringEvent, waitTimeBetweenRepetition);

        while (simulationTime < endTime) {
            boolean timeframeIsActive = EventController.timeframeIsActiveAtSimulationTime(timeframe, simulationTime);
            assertTrue(timeframeIsActive);

            simulationTime += increment;
        }
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsFalseIfWaitTimeIsZeroButSimulationTimeBeforeStartTime() {
        boolean recurringEvent = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 0.0;
        double simulationTime = 0;
        double increment = 0.05;

        EventTimeframe timeframe = new EventTimeframe(startTime, endTime, recurringEvent, waitTimeBetweenRepetition);

        while (simulationTime < startTime) {
            boolean timeframeIsActive = EventController.timeframeIsActiveAtSimulationTime(timeframe, simulationTime);
            assertFalse(timeframeIsActive);

            simulationTime += increment;
        }
    }

    @Test
    public void timeframeIsActiveAtSimulationTimeReturnsTrueIfWaitTimeIsZeroAndSimulationTimeGreaterThanStartTime() {
        boolean recurringEvent = true;
        double startTime = 0.75;
        double endTime = 1.25;
        double waitTimeBetweenRepetition = 0.0;
        double simulationTime = startTime;
        double increment = 0.10;

        EventTimeframe timeframe = new EventTimeframe(startTime, endTime, recurringEvent, waitTimeBetweenRepetition);

        while (simulationTime < 500 * endTime) {
            boolean timeframeIsActive = EventController.timeframeIsActiveAtSimulationTime(timeframe, simulationTime);
            assertTrue(timeframeIsActive);

            simulationTime += increment;
        }
    }

    @Test
    public void getEventsForTimeAlwaysCreatesASingleElapsedTimeEventWithRequestedSimulationTime() {
        boolean isRecurringEvent = false;
        ScenarioStore scenarioStoreContainingOneOneTimeEvent = getScenarioStoreContainingRecurringEvent(isRecurringEvent);

        EventController eventController = new EventController(scenarioStoreContainingOneOneTimeEvent);

        double expectedSimulationTime = 1.0;

        List<Event> activeEvents = eventController.getEventsForTime(expectedSimulationTime);

        if (activeEvents.size() == 1) {
            Event event = activeEvents.get(0);

            assertEquals(ElapsedTimeEvent.class, event.getClass());
            assertEquals(expectedSimulationTime, event.getTime(), 10e-1);
        } else {
            fail("Expected only one event for simulationTime = " + expectedSimulationTime);
        }
    }

    @Test
    public void getEventsForTimeTimestampsEachActiveEvent() {
        boolean isRecurringEvent = false;
        ScenarioStore scenarioStoreContainingOneOneTimeEvent = getScenarioStoreContainingRecurringEvent(isRecurringEvent);

        // Store one concrete one-time event in "EventInfoStore" to check if its timestamp is updated.
        WaitEvent waitEvent = new WaitEvent();
        List<Event> events = new ArrayList<>();
        events.add(waitEvent);

        EventInfo activeEventInfo = scenarioStoreContainingOneOneTimeEvent.getEventInfoStore().getEventInfos().get(0);
        activeEventInfo.setEvents(events);

        EventController eventController = new EventController(scenarioStoreContainingOneOneTimeEvent);

        double expectedSimulationTime = 5.0;

        List<Event> activeEvents = eventController.getEventsForTime(expectedSimulationTime);

        for (Event event : activeEvents) {
            assertEquals(expectedSimulationTime, event.getTime(), 10e-1);
        }

        assertEquals(2, activeEvents.size());
    }

    @Test
    public void getEventsForTimeDoesNotTimestampInactiveEvents() {
        boolean isRecurringEvent = false;
        ScenarioStore scenarioStoreContainingOneOneTimeEvent = getScenarioStoreContainingRecurringEvent(isRecurringEvent);

        // Store one concrete one-time event in "EventInfoStore" to check if its timestamp is updated.
        WaitEvent waitEvent = new WaitEvent();
        List<Event> events = new ArrayList<>();
        events.add(waitEvent);

        EventInfo inactiveEventInfo = scenarioStoreContainingOneOneTimeEvent.getEventInfoStore().getEventInfos().get(0);
        inactiveEventInfo.setEvents(events);
        inactiveEventInfo.setEventTimeframe(new EventTimeframe(0, 1, false, 0));

        EventController eventController = new EventController(scenarioStoreContainingOneOneTimeEvent);

        double expectedSimulationTime = 5.0;

        List<Event> activeEvents = eventController.getEventsForTime(expectedSimulationTime);

        assertEquals(1, activeEvents.size());
        assertEquals(0, waitEvent.getTime(), 10e-1);
    }
}