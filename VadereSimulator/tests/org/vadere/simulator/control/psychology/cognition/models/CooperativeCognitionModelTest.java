package org.vadere.simulator.control.psychology.cognition.models;

import org.junit.Before;
import org.junit.Test;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootStep;
import org.vadere.util.geometry.shapes.VPoint;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class CooperativeCognitionModelTest {

    private Pedestrian pedestrian;
    private List<Pedestrian> pedestrians;
    private List<Attributes> attributes = new LinkedList<>();

    @Before
    public void initializePedestrian() {
        int agentId = 1;
        AttributesAgent attributesAgent = new AttributesAgent(agentId);

        this.pedestrian = new Pedestrian(attributesAgent, new Random());
        this.pedestrians = List.of(pedestrian);
    }



    @Test
    public void initializeSetsTopography() {
        String fieldName = "topography";
        CooperativeCognitionModel modelUnderTest = new CooperativeCognitionModel();

        try {
            Field privateTopographyField = CooperativeCognitionModel.class.getDeclaredField(fieldName);
            privateTopographyField.setAccessible(true);

            assertNull(privateTopographyField.get(modelUnderTest));
            modelUnderTest.initialize(new Topography(), new Random(0));
            assertNotNull(privateTopographyField.get(modelUnderTest));

        } catch (IllegalAccessException ex) {
            System.out.println("This Java version forbids to access private members via reflection.");
        } catch (NoSuchFieldException ex) {
            fail(String.format("No field with name \"%s\"!", fieldName));
        }
    }

    @Test
    public void updateSetsTargetOrientedIfFootstepHistoryIsTooShort() {
        pedestrian.setSelfCategory(SelfCategory.WAIT);
        int footstepHistorySize = pedestrian.getFootstepHistory().size();

        CooperativeCognitionModel modelUnderTest = new CooperativeCognitionModel();

        for (int i = 0; i < 10; i++) {
            // Pedestrian does not move!
            modelUnderTest.update(pedestrians);
            assertEquals(0, footstepHistorySize);
            assertEquals(SelfCategory.TARGET_ORIENTED, pedestrian.getSelfCategory());
        }
    }

    @Test
    public void updateSetsTargetOrientedIfSpeedIsAboveThreshold() {
        int requiredFootsteps = 2;
        double minimumSpeedToBecomeTargetOriented = 0.6;

        pedestrian.setSelfCategory(SelfCategory.WAIT);

        CooperativeCognitionModel modelUnderTest = new CooperativeCognitionModel();

        for (int i = 0; i < 10; i++) {
            // Move pedestrian along the x-axis:
            double startTime = i;
            double startPosition = i * minimumSpeedToBecomeTargetOriented;
            double endPosition = startPosition + minimumSpeedToBecomeTargetOriented;
            FootStep footStep = new FootStep(new VPoint(startPosition, 0), new VPoint(endPosition, 0), startTime, startTime + 1);
            pedestrian.getFootstepHistory().add(footStep);

            modelUnderTest.update(pedestrians);

            if (i >= requiredFootsteps) {
                assertEquals(SelfCategory.TARGET_ORIENTED, pedestrian.getSelfCategory());
            }
        }
    }

    @Test
    public void updateSetsCooperativeIfSpeedIsBelowThreshold() {
        int requiredFootsteps = 2;
        double maximumSpeedToBecomeCooperative = 0.04;

        pedestrian.setSelfCategory(SelfCategory.WAIT);

        CooperativeCognitionModel modelUnderTest = new CooperativeCognitionModel();

        for (int i = 0; i < 10; i++) {
            // Move pedestrian along the x-axis:
            double startTime = i;
            double startPosition = i * maximumSpeedToBecomeCooperative;
            double endPosition = startPosition + maximumSpeedToBecomeCooperative;
            FootStep footStep = new FootStep(new VPoint(startPosition, 0), new VPoint(endPosition, 0), startTime, startTime + 1);
            pedestrian.getFootstepHistory().add(footStep);

            modelUnderTest.update(pedestrians);

            if (i >= requiredFootsteps) {
                assertEquals(SelfCategory.COOPERATIVE, pedestrian.getSelfCategory());
            }
        }
    }
}