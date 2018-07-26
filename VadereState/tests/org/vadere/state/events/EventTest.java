package org.vadere.state.events;

import org.junit.Before;
import org.junit.Test;
import org.vadere.state.events.types.ElapsedTimeEvent;
import org.vadere.state.events.types.Event;
import org.vadere.state.events.types.WaitEvent;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class EventTest {

    List<Event> emptyList;
    List<Event> oneElapsedTimeEvent;
    List<Event> multipleElapsedTimeEvents;
    List<Event> mixedEvents;

    @Before
    public void setUp() {
        double time = 0;
        ElapsedTimeEvent elapsedTimeEvent1 = new ElapsedTimeEvent(time);
        ElapsedTimeEvent elapsedTimeEvent2 = new ElapsedTimeEvent(time);
        WaitEvent waitEvent1 = new WaitEvent(time);

        emptyList = new ArrayList<>();
        oneElapsedTimeEvent = new ArrayList<>();
        multipleElapsedTimeEvents = new ArrayList<>();
        mixedEvents = new ArrayList<>();

        oneElapsedTimeEvent.add(elapsedTimeEvent1);

        multipleElapsedTimeEvents.add(elapsedTimeEvent1);
        multipleElapsedTimeEvents.add(elapsedTimeEvent2);

        mixedEvents.add(elapsedTimeEvent1);
        mixedEvents.add(waitEvent1);
        mixedEvents.add(elapsedTimeEvent2);
    }


    @Test
    public void listContainsEventReturnsFalseIfListIsEmpty() {
        boolean actualResult = Event.listContainsEvent(emptyList, Event.class);

        assertFalse(actualResult);
    }

    @Test
    public void listContainsEventReturnsFalseIfPassingNull() {
        boolean actualResult = Event.listContainsEvent(oneElapsedTimeEvent, null);

        assertFalse(actualResult);
    }

    @Test
    public void listContainsEventReturnsFalseIfEventNotInList() {
        boolean actualResult = Event.listContainsEvent(oneElapsedTimeEvent, WaitEvent.class);

        assertFalse(actualResult);
    }

    @Test
    public void listContainsEventReturnsTrueIfPassedEventInList() {
        boolean actualResult = Event.listContainsEvent(oneElapsedTimeEvent, ElapsedTimeEvent.class);

        assertTrue(actualResult);
    }

    @Test
    public void listContainsEventReturnsTrueIfPassedEventInListMultipleTimes() {
        boolean actualResult = Event.listContainsEvent(multipleElapsedTimeEvents, ElapsedTimeEvent.class);

        assertTrue(actualResult);
    }

    @Test
    public void listContainsEventReturnsTrueIfPassedEventIsInMixedList() {
        boolean actualResult = Event.listContainsEvent(mixedEvents, WaitEvent.class);

        assertTrue(actualResult);
    }
}