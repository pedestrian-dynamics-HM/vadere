package org.vadere.simulator.control.psychology.cognition;

import org.junit.Test;
import org.vadere.simulator.control.psychology.cognition.models.SimpleCognitionModel;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class SimpleCognitionModelTest {

    private List<Attributes> attributes = new LinkedList<>();

    private Topography createTopography() {
        return new Topography();
    }

    private List<Pedestrian> createPedestrians(int totalPedestrians) {
        List<Pedestrian> pedestrians = new ArrayList<>();

        long seed = 0;
        Random random = new Random(seed);

        for (int i = 0; i < totalPedestrians; i++) {
            AttributesAgent attributesAgent = new AttributesAgent(i);
            Pedestrian pedestrian = new Pedestrian(attributesAgent, random);
            pedestrian.setPosition(new VPoint(i, i));
            
            pedestrians.add(pedestrian);
        }

        return pedestrians;
    }

    private void testCognitionLayer(SelfCategory defaultSelfCategory, SelfCategory expectedSelfCategory, Stimulus presentStimulus) {
        List<Pedestrian> pedestrians = createPedestrians(2);

        for (Pedestrian pedestrian : pedestrians) {
            pedestrian.setMostImportantStimulus(presentStimulus);
            pedestrian.setSelfCategory(defaultSelfCategory);
        }

        SimpleCognitionModel simpleCognitionModel = new SimpleCognitionModel();
        simpleCognitionModel.initialize(createTopography(), new Random(0));

        simpleCognitionModel.update(pedestrians);

        for (Pedestrian pedestrian : pedestrians) {
            assertEquals(expectedSelfCategory, pedestrian.getSelfCategory());
        }
    }

    @Test
    public void updatesSetsChangeTargetOnChangeTargetStimulus() {
        testCognitionLayer(SelfCategory.TARGET_ORIENTED,
                SelfCategory.CHANGE_TARGET,
                new ChangeTarget());
    }

    @Test
    public void updatesSetsThreatenedOnThreatStimulus() {
        testCognitionLayer(SelfCategory.TARGET_ORIENTED,
                SelfCategory.THREATENED,
                new Threat());
    }

    @Test
    public void updatesSetsWaitOnWaitStimulus() {
        testCognitionLayer(SelfCategory.TARGET_ORIENTED,
                SelfCategory.WAIT,
                new Wait());
    }

    @Test
    public void updatesSetsWaitOnWaitInAreaStimulus() {
        testCognitionLayer(SelfCategory.TARGET_ORIENTED,
                SelfCategory.WAIT,
                new WaitInArea());
    }

    @Test
    public void updatesSetsTargetOrientedOnElapsedTime() {
        testCognitionLayer(SelfCategory.WAIT,
                SelfCategory.TARGET_ORIENTED,
                new ElapsedTime());
    }

}