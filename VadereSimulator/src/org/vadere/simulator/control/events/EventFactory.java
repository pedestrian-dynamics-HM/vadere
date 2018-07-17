package org.vadere.simulator.control.events;


import org.vadere.state.events.ElapsedTimeEvent;
import org.vadere.state.events.Event;
import org.vadere.state.events.WaitEvent;

/**
 * This class is used to create events from @see org.vadere.state.events package.
 */
public class EventFactory {
    int totalEvents;

    public EventFactory() {
        totalEvents = 0;
    }

    public Event getEvent(Class clazz, double time) {
        Event currentEvent = null;

        if (clazz.equals(ElapsedTimeEvent.class)) {
            currentEvent = new ElapsedTimeEvent(time);
        } else if (clazz.equals(WaitEvent.class)) {
            currentEvent = new WaitEvent(time);
        } else {
            throw new IllegalArgumentException("Class type not supported: "  + clazz.getName());
        }

        return currentEvent;
    }
}
