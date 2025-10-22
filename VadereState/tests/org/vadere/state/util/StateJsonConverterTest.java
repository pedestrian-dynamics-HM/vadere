package org.vadere.state.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.distributions.AttributesBinomialDistribution;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.models.airflow.AttributesAirFlowModel;
import org.vadere.state.attributes.models.airflow.AttributesBounds;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.attributes.spawner.AttributesRegularSpawner;
import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.state.psychology.perception.json.StimulusInfo;
import org.vadere.state.psychology.perception.json.StimulusInfoStore;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.psychology.perception.types.Timeframe;
import org.vadere.state.psychology.perception.types.WaitInArea;
import org.vadere.state.scenario.*;
import org.vadere.state.scenario.*;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.List;

import static  org.junit.jupiter.api.Assertions.assertEquals;
import static  org.junit.jupiter.api.Assertions.assertNotEquals;

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

        assertEquals(hash1, hash2,"Hashes must match");
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

        assertNotEquals(hash1, hash2,"Hashes must differ");
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

        assertNotEquals(hash1, hash2,"Hashes must differ");
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

        assertEquals(hash1, hash2,"Hashes must differ");

        // changes must change the floor field hash
        attrTarget.setShape(new VRectangle(2,2,2,2));
        String hash3 = StateJsonConverter.getFloorFieldHash(topography, attr);
        assertNotEquals(hash1, hash3,"Hashes must differ");
    }

    @Test
    public void getLocomotionHashTest() {
        // Setup initial topography
        Topography topography = new Topography();
        topography.addObstacle(new Obstacle(new AttributesObstacle(3, new VRectangle(1, 1, 3, 3))));

        AttributesTarget attrTarget = new AttributesTarget(-1, new VRectangle(1, 1, 1, 1));
        Target t = new Target(attrTarget);
        topography.addTarget(t);

        // Setup source
        AttributesSource attrSource = new AttributesSource(-1);
        attrSource.setShape(new VRectangle(5, 5, 2, 2));
        Source source = new Source(attrSource);
        topography.addSource(source);

        long simulationSeed = 12345L;
        List<Attributes> attributesFallbackModel = new ArrayList<>();
        // Add appropriate fallback model attributes based on your locomotion model
        // Example: attributesFallbackModel.add(new AttributesOSM());

        String hash1 = StateJsonConverter.getLocomotionHash(topography, simulationSeed, attributesFallbackModel);

        // Changes that should not change the locomotion hash (CacheViewExclude properties)
        topography.addAerosolCloud(new AerosolCloud());

        String hash2 = StateJsonConverter.getLocomotionHash(topography, simulationSeed, attributesFallbackModel);
        assertEquals(hash1, hash2, "Hashes must be equal - source spawn parameters should not affect locomotion hash");

        // Changes that SHOULD change the locomotion hash

        // Test 1: Changing source shape (affects topography structure)
        attrSource.setShape(new VRectangle(6, 6, 3, 3));
        String hash3 = StateJsonConverter.getLocomotionHash(topography, simulationSeed, attributesFallbackModel);
        assertNotEquals(hash1, hash3, "Hashes must differ - source shape affects locomotion");
        // Reset shape
        attrSource.setShape(new VRectangle(5, 5, 2, 2));

        // Test 2: Changing simulation seed
        long differentSeed = 54321L;
        String hash4 = StateJsonConverter.getLocomotionHash(topography, differentSeed, attributesFallbackModel);
        assertNotEquals(hash1, hash4, "Hashes must differ - simulation seed affects locomotion");

        // Test 3: Changing topography structure (obstacle)
        topography.addObstacle(new Obstacle(new AttributesObstacle(4, new VRectangle(10, 10, 2, 2))));
        String hash5 = StateJsonConverter.getLocomotionHash(topography, simulationSeed, attributesFallbackModel);
        assertNotEquals(hash1, hash5, "Hashes must differ - topography structure affects locomotion");

        // Test 4: Changing fallback model attributes
        List<Attributes> differentFallbackModel = new ArrayList<>();
        // Add different attributes or modify existing ones
        String hash6 = StateJsonConverter.getLocomotionHash(topography, simulationSeed, differentFallbackModel);
        assertNotEquals(hash1, hash6, "Hashes must differ - fallback model attributes affect locomotion");

        // Test 5: Changing spawner attributes
        attrSource.setId(99);
        AttributesRegularSpawner attrSpawn = new AttributesRegularSpawner();
        attrSpawn.setConstraintsElementsMax(130);
        attrSpawn.setConstraintsTimeEnd(2.0);
        attrSpawn.setConstraintsTimeEnd(20.0);
        attrSource.setSpawnerAttributes(attrSpawn);
        String hash7 = StateJsonConverter.getLocomotionHash(topography, simulationSeed, differentFallbackModel);
        assertNotEquals(hash1, hash7, "Hashes must differ - spawner attributes affect locomotion");
    }


    @Test
    public void getAirFlowHashTest() {
        // Setup initial topography
        Topography topography = new Topography();
        AttributesObstacle attrObs1 = new AttributesObstacle(1, new VRectangle(1, 1, 3, 3));
        Obstacle obstacle1 = new Obstacle(attrObs1);
        topography.addObstacle(obstacle1);

        // Setup initial AirFlow model
        AttributesAirFlowModel attrAirFlow = new AttributesAirFlowModel();
        String hash1 = StateJsonConverter.getAirFlowHash(topography, attrAirFlow);

        // Changes that should NOT change the AirFlow hash

        // Test 1: Adding a Target
        AttributesTarget attrTarget = new AttributesTarget(1, new VRectangle(8, 8, 1, 1));
        Target t = new Target(attrTarget);
        topography.addTarget(t);

        String hash2 = StateJsonConverter.getAirFlowHash(topography, attrAirFlow);
        assertEquals(hash1, hash2, "Hashes must be equal - Targets should not affect airflow hash");

        // Test 2: Adding a Source
        AttributesSource attrSource = new AttributesSource(1, new VRectangle(5, 5, 2, 2));
        Source source = new Source(attrSource);
        topography.addSource(source);

        String hash3 = StateJsonConverter.getAirFlowHash(topography, attrAirFlow);
        assertEquals(hash1, hash3, "Hashes must be equal - Sources should not affect airflow hash");

        // Test 3: Adding an AerosolCloud
        topography.addAerosolCloud(new AerosolCloud());

        String hash4 = StateJsonConverter.getAirFlowHash(topography, attrAirFlow);
        assertEquals(hash1, hash4, "Hashes must be equal - AerosolClouds should not affect airflow hash");

        // Changes that SHOULD change the AirFlow hash

        // Test 4: Changing AirFlow model attributes
        AttributesAirFlowModel differentAttrAirFlow = new AttributesAirFlowModel();
        differentAttrAirFlow.setBounds(new AttributesBounds(1, 9, 1, 9));
        String hash5 = StateJsonConverter.getAirFlowHash(topography, differentAttrAirFlow);
        assertNotEquals(hash1, hash5, "Hashes must differ - AirFlow model attributes affect the hash");

        // Test 5: Changing topography structure (adding an obstacle)
        topography.addObstacle(new Obstacle(new AttributesObstacle(2, new VRectangle(20, 20, 5, 5))));
        String hash6 = StateJsonConverter.getAirFlowHash(topography, attrAirFlow);
        assertNotEquals(hash1, hash6, "Hashes must differ - adding an obstacle affects topography structure");
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