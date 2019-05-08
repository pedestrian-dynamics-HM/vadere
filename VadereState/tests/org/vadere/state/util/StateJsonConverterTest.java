package org.vadere.state.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vadere.state.events.json.EventInfo;
import org.vadere.state.events.json.EventInfoStore;
import org.vadere.state.events.types.Event;
import org.vadere.state.events.types.EventTimeframe;
import org.vadere.state.events.types.WaitInAreaEvent;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class StateJsonConverterTest {

    @NotNull
    private EventInfoStore getEventInfoStore() {
        // Create "EventTimeframe" and "Event" objects and encapsulate them in "EventInfo" objects.
        EventTimeframe eventTimeframe = new EventTimeframe(5, 30, false, 0);

        List<Event> events = new ArrayList<>();
        events.add(new WaitInAreaEvent(0, new VRectangle(12.5, 0, 5, 6)));

        EventInfo eventInfo1 = new EventInfo();
        eventInfo1.setEventTimeframe(eventTimeframe);
        eventInfo1.setEvents(events);

        List<EventInfo> eventInfos = new ArrayList<>();
        eventInfos.add(eventInfo1);

        EventInfoStore eventInfoStore = new EventInfoStore();
        eventInfoStore.setEventInfos(eventInfos);

        return eventInfoStore;
    }

    @Test
    public void deserializeEventsFromArrayNodeReturnsEmptyEventInfoStoreIfPassingNullNode() {
        int expectedSize = 0;

        EventInfoStore eventInfoStore = StateJsonConverter.deserializeEventsFromArrayNode(null);

        assertEquals(expectedSize, eventInfoStore.getEventInfos().size());
    }

    @Test
    public void deserializeEventsFromArrayNodeReturnsEventInfoStoreIfPassingValidArrayNode() {
        EventInfoStore expectedEventInfoStore = getEventInfoStore();
        ObjectMapper mapper = new JacksonObjectMapper();
        JsonNode jsonNode = mapper.convertValue(expectedEventInfoStore, JsonNode.class);

        EventInfoStore actualEventInfoStore = StateJsonConverter.deserializeEventsFromArrayNode(jsonNode.get("eventInfos"));

        EventInfo expectedEventInfo = expectedEventInfoStore.getEventInfos().get(0);
        EventInfo actualEventInfo = actualEventInfoStore.getEventInfos().get(0);

        double allowedDelta = 1e-3;

        assertEquals(expectedEventInfo.getEventTimeframe().getStartTime(), actualEventInfo.getEventTimeframe().getStartTime(), allowedDelta);
        assertEquals(expectedEventInfo.getEventTimeframe().getEndTime(), actualEventInfo.getEventTimeframe().getEndTime(), allowedDelta);
        assertEquals(expectedEventInfo.getEventTimeframe().isRepeat(), actualEventInfo.getEventTimeframe().isRepeat());
        assertEquals(expectedEventInfo.getEventTimeframe().getWaitTimeBetweenRepetition(), actualEventInfo.getEventTimeframe().getWaitTimeBetweenRepetition(), allowedDelta);
    }

    @Test
    public void deserializeEvents() {
    }

    @Test
    public void serializeEvents() {
    }

    @Test
    public void serializeEventsToNode() {
    }
}