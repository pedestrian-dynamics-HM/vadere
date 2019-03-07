package org.vadere.state.events.presettings;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.vadere.state.events.json.EventInfo;
import org.vadere.state.events.json.EventInfoStore;
import org.vadere.state.events.types.*;
import org.vadere.state.util.JacksonObjectMapper;
import org.vadere.util.geometry.shapes.VRectangle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provide JSON presettings for commonly used events.
 *
 * This class can be used as helper for GUI elements.
 */
public class EventPresettings {
    /** Map an event class (e.g., BangEvent) to a JSON string. */
    public static Map<Class, String> PRESETTINGS_MAP;

    // Static initializer for "PRESETTINGS_MAP".
    static {
        PRESETTINGS_MAP = new HashMap<>();

        Event[] eventsToUse = new Event[] {
                new BangEvent(),
                new WaitEvent(),
                new WaitInAreaEvent(0, new VRectangle(0, 0, 10, 10)),
        };

        for (Event event : eventsToUse) {
            // Container for a timeframe and the corresponding events.
            EventInfo eventInfo = new EventInfo();

            List<Event> events = new ArrayList<>();
            events.add(event);

            eventInfo.setEventTimeframe(new EventTimeframe(0, 10, false, 0));
            eventInfo.setEvents(events);

            // Container for multiple event infos.
            List<EventInfo> eventInfos = new ArrayList<>();
            eventInfos.add(eventInfo);

            EventInfoStore eventInfoStore = new EventInfoStore();
            eventInfoStore.setEventInfos(eventInfos);

            try {
                ObjectMapper mapper = new JacksonObjectMapper();
                // String jsonDataString = mapper.writeValueAsString(eventInfoStore);
                String jsonDataString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventInfoStore);

                PRESETTINGS_MAP.put(event.getClass(), jsonDataString);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
