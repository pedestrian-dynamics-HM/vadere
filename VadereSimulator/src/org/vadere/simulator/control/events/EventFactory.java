package org.vadere.simulator.control.events;


import org.vadere.state.events.types.ElapsedTimeEvent;
import org.vadere.state.events.types.Event;
import org.vadere.state.events.types.WaitEvent;
import org.vadere.state.events.types.WaitInAreaEvent;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

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
        } else if (clazz.equals(WaitInAreaEvent.class)) {
            // TODO Replace dummy area here.
            VShape area = new VRectangle(12.5, 0, 5, 6);
            currentEvent = new WaitInAreaEvent(time, area);
        }
        else {
            throw new IllegalArgumentException("Class type not supported: "  + clazz.getName());
        }

        return currentEvent;
    }
}
