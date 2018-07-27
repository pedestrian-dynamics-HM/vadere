package org.vadere.state.events.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.vadere.state.events.types.ElapsedTimeEvent;
import org.vadere.state.events.types.Event;
import org.vadere.state.events.types.EventTimeframe;
import org.vadere.state.events.types.WaitInAreaEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class bundles multiple @see EventInfo objects.
 *
 * This class is just a wrapper to get a convenient JSON de-/serialization. The JSON serialization should look like
 * this:
 *
 *      "eventInfos": [
 *                {
 *                     "eventTimeframe": {
 *                         "startTime":...,
 *                         "endTime":...,
 *                         "repeat":...,
 *                         "waitTimeBetweenRepetition":...
 *                     },
 *                     "events": [
 *                         {"type":"ElapsedTimeEvent","targets":[...]},
 *                         {"type":"WaitInAreaEvent","targets":[...],"area":...},
 *                         ...
 *                     ]
 *                },
 *                {
 *                    ...
 *                }
 *      ]
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

        // Create "EventTimeframe" and "Event" objects.
        EventTimeframe eventTimeframe = new EventTimeframe();

        List<Event> events = new ArrayList<>();
        events.add(new ElapsedTimeEvent(0));
        events.add(new WaitInAreaEvent(1));

        // Wrap "EventTimeframe" and "Event" objects in two "EventInfo" objects.
        EventInfo eventInfo1 = new EventInfo();
        eventInfo1.setEventTimeframe(eventTimeframe);
        eventInfo1.setEvents(events);

        EventInfo eventInfo2 = new EventInfo();
        eventInfo2.setEventTimeframe(eventTimeframe);
        eventInfo2.setEvents(events);

        List<EventInfo> eventInfos = new ArrayList<>();
        eventInfos.add(eventInfo1);
        eventInfos.add(eventInfo2);

        // Wrap "EventInfo" objects in "EventInfoStore".
        EventInfoStore eventInfoStore = new EventInfoStore();
        eventInfoStore.setEventInfos(eventInfos);

        // Use annotations at event classes to specify how JSON <-> Java mapping should look like.
        ObjectMapper mapper = new ObjectMapper();

        try {
            String jsonDataString = mapper.writeValueAsString(eventInfoStore);
            System.out.println(jsonDataString);

            EventInfoStore deserializedEventInfoStore = mapper.readValue(jsonDataString, EventInfoStore.class);
            for (EventInfo eventInfo : deserializedEventInfoStore.getEventInfos()) {
                System.out.print(eventInfo.getEventTimeframe());
                System.out.print(eventInfo.getEvents());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
