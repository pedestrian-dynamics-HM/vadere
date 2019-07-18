package org.vadere.state.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.events.json.EventInfo;
import org.vadere.state.events.json.EventInfoStore;
import org.vadere.state.events.types.Event;
import org.vadere.state.events.types.EventTimeframe;
import org.vadere.state.events.types.WaitInAreaEvent;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VRectangle;

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
    public void getFloorFieldHashTest1(){
        Topography topography = new Topography();
        topography.addObstacle(new Obstacle(new AttributesObstacle(3, new VRectangle(1,1,3,3))));
        AttributesFloorField attr = new AttributesFloorField();
        attr.setCacheDir("some/cache/dir");
        String hash1 = StateJsonConverter.getFloorFieldHash(topography, attr);

        // changes to cacheDir should not have any influence to the floor field hash
        attr.setCacheDir("some/other/cache/dir");
        String hash2 = StateJsonConverter.getFloorFieldHash(topography, attr);

        assertEquals("Hashes must match",hash1, hash2);
    }

    @Test
    public void getFloorFieldHashTest2(){
        Topography topography = new Topography();
        topography.addObstacle(new Obstacle(new AttributesObstacle(3, new VRectangle(1,1,3,3))));
        AttributesFloorField attr = new AttributesFloorField();
        attr.setCacheDir("some/cache/dir");
        String hash1 = StateJsonConverter.getFloorFieldHash(topography, attr);

        // changes to anything other thatn  cacheDir must change the floor field hash
        attr.setObstacleGridPenalty(23.3);
        String hash2 = StateJsonConverter.getFloorFieldHash(topography, attr);

        assertNotEquals("Hashes must differ",hash1, hash2);
    }

    @Test
    public void getFloorFieldHashTest3(){
        Topography topography = new Topography();
        topography.addObstacle(new Obstacle(new AttributesObstacle(3, new VRectangle(1,1,3,3))));
        AttributesFloorField attr = new AttributesFloorField();
        attr.setCacheDir("some/cache/dir");
        String hash1 = StateJsonConverter.getFloorFieldHash(topography, attr);

        // changes to anything other thatn  cacheDir must change the floor field hash
        topography.addObstacle(new Obstacle(new AttributesObstacle(3, new VRectangle(3,3,1,1))));
        String hash2 = StateJsonConverter.getFloorFieldHash(topography, attr);

        assertNotEquals("Hashes must differ",hash1, hash2);
    }

    @Test
    public void getFloorFieldHashTestAttTarget(){
        Topography topography = new Topography();
        topography.addObstacle(new Obstacle(new AttributesObstacle(3, new VRectangle(1,1,3,3))));
        AttributesFloorField attr = new AttributesFloorField();
        AttributesTarget attrTarget = new AttributesTarget(new VRectangle(1,1,1,1));
        Target t = new Target(attrTarget);
        topography.addTarget(t);
        String hash1 = StateJsonConverter.getFloorFieldHash(topography, attr);

        // changes must NOT change the floor field hash
        attrTarget.setId(33);
        attrTarget.setAbsorbing(false);
        attrTarget.setWaitingTime(3);
        attrTarget.setWaitingTimeYellowPhase(2);
        attrTarget.setParallelWaiters(1);
        attrTarget.setIndividualWaiting(false);
        attrTarget.setDeletionDistance(0.4);
        attrTarget.setStartingWithRedLight(true);
        attrTarget.setNextSpeed(1);
        String hash2 = StateJsonConverter.getFloorFieldHash(topography, attr);

        assertEquals("Hashes must differ",hash1, hash2);

        // changes must change the floor field hash
        attrTarget.setShape(new VRectangle(2,2,2,2));
        String hash3 = StateJsonConverter.getFloorFieldHash(topography, attr);
        assertNotEquals("Hashes must differ",hash1, hash3);
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