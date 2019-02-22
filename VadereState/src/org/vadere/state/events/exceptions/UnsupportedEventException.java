package org.vadere.state.events.exceptions;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.events.types.ElapsedTimeEvent;
import org.vadere.state.events.types.Event;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

/**
 * Use this exception if an event-handling class does not support a specific event.
 */
public class UnsupportedEventException extends RuntimeException {

    public UnsupportedEventException(@NotNull Event unsupportedEvent, @NotNull Class implementingClass) {
        super(String.format("Event \"%s\" not supported by class \"%s\"!",
                unsupportedEvent.getClass().getSimpleName(),
                implementingClass.getSimpleName())
        );
    }

    public static void throwIfNotElapsedTimeEvent(Collection<? extends Pedestrian> pedestrians, Class caller) {
        for (Pedestrian pedestrian : pedestrians) {
            Event currentEvent = pedestrian.getMostImportantEvent();

            if ((currentEvent instanceof ElapsedTimeEvent) == false) {
                throw new UnsupportedEventException(currentEvent, caller);
            }
        }
    }
}
