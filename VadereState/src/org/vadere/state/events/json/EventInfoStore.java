package org.vadere.state.events.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.vadere.state.events.types.ElapsedTimeEvent;
import org.vadere.state.events.types.Event;
import org.vadere.state.events.types.EventTimeframe;
import org.vadere.state.events.types.WaitInAreaEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * This class bundles multiple @see EventInfo objects.
 *
 * This class is just a wrapper to get a convenient JSON de-/serialization. The JSON serialization should look like
 * this:
 *
 * [
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
 *      },
 *      {
 *          ...
*       }
 * ]
 */
public class EventInfoStore {

    private List<EventInfo> eventInfos;

    public List<EventInfo> getEventInfos() {
        return eventInfos;
    }

    public void setEventInfos(List<EventInfo> eventInfos) {
        this.eventInfos = eventInfos;
    }

    public static void main(String... args) {
        // TODO Remove main method here.

        EventTimeframe eventTimeframe = new EventTimeframe();
        Event elapsedTimeEvent = new ElapsedTimeEvent(0);
        Event waitInAreaEvent = new WaitInAreaEvent(10);

        List<Event> events = new ArrayList<>();
        events.add(elapsedTimeEvent);
        events.add(waitInAreaEvent);

        EventInfo eventInfo1 = new EventInfo();
        eventInfo1.setEventTimeframe(eventTimeframe);
        eventInfo1.setEvents(events);

        EventInfo eventInfo2 = new EventInfo();
        eventInfo2.setEventTimeframe(eventTimeframe);
        eventInfo2.setEvents(events);

        List<EventInfo> eventInfos = new ArrayList<>();
        eventInfos.add(eventInfo1);
        eventInfos.add(eventInfo2);

        // Use annotations at event classes to specify how JSON <-> Java mapping should look like.
        ObjectMapper mapper = new ObjectMapper();
        // mapper.enableDefaultTyping();

        try {
            String jsonDataString = mapper.writeValueAsString(eventInfos);
            System.out.println(jsonDataString);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
