package org.vadere.state.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.vadere.state.attributes.distributions.AttributesBinomialDistribution;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.psychology.perception.json.StimulusInfo;
import org.vadere.state.psychology.perception.json.StimulusInfoStore;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.psychology.perception.types.Timeframe;
import org.vadere.state.psychology.perception.types.WaitInArea;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class StateJsonConverterTest {

    @NotNull
    private StimulusInfoStore getEventInfoStore() {
        // Create "Timeframe" and "Stimulus" objects and encapsulate them in "StimulusInfo" objects.
        Timeframe timeframe = new Timeframe(5, 30, false, 0);

        List<Stimulus> stimuli = new ArrayList<>();
        stimuli.add(new WaitInArea(0, new VRectangle(12.5, 0, 5, 6)));

        StimulusInfo stimulusInfo1 = new StimulusInfo();
        stimulusInfo1.setTimeframe(timeframe);
        stimulusInfo1.setStimuli(stimuli);

        List<StimulusInfo> stimulusInfos = new ArrayList<>();
        stimulusInfos.add(stimulusInfo1);

        StimulusInfoStore stimulusInfoStore = new StimulusInfoStore();
        stimulusInfoStore.setStimulusInfos(stimulusInfos);

        return stimulusInfoStore;
    }

    @Test
    public void deserializeEventsFromArrayNodeReturnsEmptyEventInfoStoreIfPassingNullNode() {
        int expectedSize = 0;

        StimulusInfoStore stimulusInfoStore = StateJsonConverter.deserializeStimuliFromArrayNode(null);

        assertEquals(expectedSize, stimulusInfoStore.getStimulusInfos().size());
    }

    @Test
    public void deserializeEventsFromArrayNodeReturnsEventInfoStoreIfPassingValidArrayNode() {
        StimulusInfoStore expectedStimulusInfoStore = getEventInfoStore();
        ObjectMapper mapper = new JacksonObjectMapper();
        JsonNode jsonNode = mapper.convertValue(expectedStimulusInfoStore, JsonNode.class);

        StimulusInfoStore actualStimulusInfoStore = StateJsonConverter.deserializeStimuliFromArrayNode(jsonNode);

        StimulusInfo expectedStimulusInfo = expectedStimulusInfoStore.getStimulusInfos().get(0);
        StimulusInfo actualStimulusInfo = actualStimulusInfoStore.getStimulusInfos().get(0);

        double allowedDelta = 1e-3;

        assertEquals(expectedStimulusInfo.getTimeframe().getStartTime(), actualStimulusInfo.getTimeframe().getStartTime(), allowedDelta);
        assertEquals(expectedStimulusInfo.getTimeframe().getEndTime(), actualStimulusInfo.getTimeframe().getEndTime(), allowedDelta);
        assertEquals(expectedStimulusInfo.getTimeframe().isRepeat(), actualStimulusInfo.getTimeframe().isRepeat());
        assertEquals(expectedStimulusInfo.getTimeframe().getWaitTimeBetweenRepetition(), actualStimulusInfo.getTimeframe().getWaitTimeBetweenRepetition(), allowedDelta);
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
        AttributesTarget attrTarget = new AttributesTarget(-1,new VRectangle(1,1,1,1));
        Target t = new Target(attrTarget);
        topography.addTarget(t);
        String hash1 = StateJsonConverter.getFloorFieldHash(topography, attr);

        // changes must NOT change the floor field hash
        attrTarget.setId(33);
        attrTarget.setAbsorbing(false);
        //attrTarget.setWaitingBehaviour(Target.WaitingBehaviour.TRAFFIC_LIGHT);
        //attrTarget.setWaitingTimeYellowPhase(2);
        attrTarget.setParallelEvents(1);
        attrTarget.getWaiterAttributes().setDistribution(new AttributesBinomialDistribution());
        attrTarget.getAbsorberAttributes().setDeletionDistance(0.4);
        //attrTarget.setStartingWithRedLight(true);
        attrTarget.setLeavingSpeed(1.0);
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