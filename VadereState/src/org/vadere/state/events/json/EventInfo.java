package org.vadere.state.events.json;

import org.vadere.state.events.types.Event;
import org.vadere.state.events.types.EventTimeframe;

import java.util.List;

/**
 * This class bundles one @see EventTimeframe and a list of @see Event objects.
 * I.e., multiple events can occur in a specified timeframe.
 *
 * This class is just a wrapper to get a convenient JSON de-/serialization. The JSON serialization should look like
 * this:
 *
 *      {
 *           "eventTimeframe": {
 *               "startTime":...,
 *               "endTime":...,
 *               "repeat":...,
 *               "waitTimeBetweenRepetition":...
 *           },
 *           "events": [
 *               {"type":"ElapsedTimeEvent","targets":[...]},
 *               {"type":"WaitInAreaEvent","targets":[...],"area":...},
 *               ...
 *           ]
 *      }
 */
public class EventInfo {

    private EventTimeframe eventTimeframe;
    private List<Event> events;

    public EventTimeframe getEventTimeframe() {
        return eventTimeframe;
    }

    public void setEventTimeframe(EventTimeframe eventTimeframe) {
        this.eventTimeframe = eventTimeframe;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

}
