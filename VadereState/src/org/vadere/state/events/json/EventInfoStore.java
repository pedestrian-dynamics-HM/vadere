package org.vadere.state.events.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.vadere.state.events.types.*;
import org.vadere.state.util.JacksonObjectMapper;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
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

    public EventInfoStore() {
        this.eventInfos = new ArrayList<>();
    }

    public List<EventInfo> getEventInfos() {
        return eventInfos;
    }

    public void setEventInfos(List<EventInfo> eventInfos) {
        this.eventInfos = eventInfos;
    }

    public static void main(String... args) {
        // Create "EventTimeframe" and "Event" objects and encapsulate them in "EventInfo" objects.
        EventTimeframe eventTimeframe = new EventTimeframe(5, 30, false, 0);

        List<Event> events = new ArrayList<>();
        events.add(new WaitEvent());
        events.add(new WaitInAreaEvent(0, new VRectangle(12.5, 0, 5, 6)));
        // events.add(new WaitInAreaEvent(0, new VCircle(5, 5, 5)));

        EventInfo eventInfo1 = new EventInfo();
        eventInfo1.setEventTimeframe(eventTimeframe);
        eventInfo1.setEvents(events);

        List<EventInfo> eventInfos = new ArrayList<>();
        eventInfos.add(eventInfo1);

        EventInfoStore eventInfoStore = new EventInfoStore();
        eventInfoStore.setEventInfos(eventInfos);

        // Use annotations at event classes to specify how JSON <-> Java mapping should look like.
        // "VShape" are mapped by "JacksonObjectMapper" implementation.
        ObjectMapper mapper = new JacksonObjectMapper();

        // De/-Serialize an "EventInfoStore":
        try {
            String jsonDataString = mapper.writeValueAsString(eventInfoStore);

            System.out.println("Serialized \"EventInfoStore\":");
            System.out.println(jsonDataString);
            System.out.println();

            System.out.println("Serialized \"EventInfo\" elements in the \"EventInfoStore\":");
            EventInfoStore deserializedEventInfoStore = mapper.readValue(jsonDataString, EventInfoStore.class);
            for (EventInfo eventInfo : deserializedEventInfoStore.getEventInfos()) {
                System.out.print(eventInfo.getEventTimeframe());
                System.out.print(eventInfo.getEvents());
            }
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Serialize specific events:
        System.out.println("Examples of some event serializations:");

        try {
            LinkedList<Integer> newTargetIds = new LinkedList<>();
            newTargetIds.add(1);
            newTargetIds.add(2);

            String changeTargetEventAsJsonString = mapper.writeValueAsString(new ChangeTargetEvent(0.0, newTargetIds));
            System.out.println(changeTargetEventAsJsonString);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }
}
