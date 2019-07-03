package org.vadere.state.events.types;

import org.jetbrains.annotations.NotNull;

/**
 * An event factory to convert strings into {@link Event} objects.
 *
 * This is required to parse the output of output processors.
 */
public class EventFactory {

    public static Event stringToEvent(@NotNull String eventAsString) {
        Event eventObject = null;

        if (eventAsString.matches(BangEvent.class.getSimpleName())) {
            eventObject = new BangEvent();
        } else if (eventAsString.matches(ElapsedTimeEvent.class.getSimpleName())) {
            eventObject = new ElapsedTimeEvent();
        } else if (eventAsString.matches(WaitEvent.class.getSimpleName())) {
            eventObject = new WaitEvent();
        } else if (eventAsString.matches(WaitInAreaEvent.class.getSimpleName())) {
            eventObject = new WaitInAreaEvent();
        }

        return eventObject;
    }
}
