package org.vadere.simulator.control.cognition;

import org.vadere.state.events.types.*;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The EventCognition class should provide logic to prioritize {@link Event}s for a pedestrian based on its
 * current state/attributes (e.g., a {@link BangEvent} is more important than a {@link WaitEvent}.
 */
public class EventCognition {

    public void prioritizeEventsForPedestrians(List<Event> events, Collection<Pedestrian> pedestrians){
        for (Pedestrian pedestrian : pedestrians) {
            // TODO: prioritize the events for the current time step for each pedestrian individually.
            // by using a finite state machine, weight pedestrian's attributes or any other good mechanism.
            Event mostImportantEvent = rankWaitHigherThanElapsedTime(events, pedestrian);
            pedestrian.setMostImportantEvent(mostImportantEvent);
        }
    }

    private Event rankWaitHigherThanElapsedTime(List<Event> events, Pedestrian pedestrian) {
        // TODO: replace dummy implementation here.
        Event mostImportantEvent = events.stream()
                .filter(event -> event instanceof ElapsedTimeEvent)
                .collect(Collectors.toList())
                .get(0);

        List<Event> waitEvents = events.stream().filter(event -> event instanceof WaitEvent).collect(Collectors.toList());
        List<Event> waitInAreaEvents = events.stream().filter(event -> event instanceof WaitInAreaEvent).collect(Collectors.toList());
        List<Event> bangEvents = events.stream().filter(event -> event instanceof BangEvent).collect(Collectors.toList());

        if (bangEvents.size() >= 1) {
            mostImportantEvent = bangEvents.get(0);
        } else if (waitEvents.size() >= 1) {
            mostImportantEvent = waitEvents.get(0);
        } else if (waitInAreaEvents.size() >= 1) {
            for (Event event : waitInAreaEvents) {
                WaitInAreaEvent waitInAreaEvent = (WaitInAreaEvent) event;

                boolean pedInArea = waitInAreaEvent.getArea().contains(pedestrian.getPosition());

                if (pedInArea) {
                    mostImportantEvent = waitInAreaEvent;
                }
            }
        }

        return mostImportantEvent;
    }

}
