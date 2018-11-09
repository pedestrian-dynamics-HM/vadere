package org.vadere.simulator.control.cognition;

import org.vadere.state.events.types.ElapsedTimeEvent;
import org.vadere.state.events.types.Event;
import org.vadere.state.events.types.WaitEvent;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The CognitionLayer class should provide logic to prioritize {@link Event}
 * objects based on attributes of a pedestrian.
 */
public class CognitionLayer {

    public void prioritizeEventsForPedestrians(List<Event> events, Collection<Pedestrian> pedestrians){
        for (Pedestrian pedestrian : pedestrians) {
            // TODO: prioritize the events for the current time step for each pedestrian individually.
            // by using a finite state machine, weight pedestrian's attributes or any other good mechanism.
            Event mostImportantEvent = rankWaitHigherThanElapsedTime(events);
            pedestrian.setMostImportantEvent(mostImportantEvent);
        }
    }

    private Event rankWaitHigherThanElapsedTime(List<Event> events) {
        // TODO: replace dummy implementation here.
        Event mostImportantEvent = null;

        List<Event> waitEvents = events.stream().filter(event -> event instanceof WaitEvent).collect(Collectors.toList());

        if (waitEvents.size() >= 1) {
            mostImportantEvent = waitEvents.get(0);
        } else {
            List<Event> elapsedTimeEvents = events.stream().filter(event -> event instanceof ElapsedTimeEvent).collect(Collectors.toList());
            mostImportantEvent = elapsedTimeEvents.get(0);
        }

        return mostImportantEvent;
    }

}
