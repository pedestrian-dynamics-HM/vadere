package org.vadere.simulator.control.psychology.cognition.models;

import org.junit.Before;
import org.junit.Test;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.psychology.cognition.GroupMembership;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.ElapsedTime;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.psychology.perception.types.Threat;
import org.vadere.state.psychology.perception.types.Wait;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootStep;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class ThreatCognitionModelTest {

    private Pedestrian pedestrian;
    private List<Pedestrian> pedestrians;
    private Topography topography;

    @Before
    public void initializePedestrian() {
        int agentId = 1;
        AttributesAgent attributesAgent = new AttributesAgent(agentId);

        this.pedestrian = new Pedestrian(attributesAgent, new Random());
        this.pedestrians = List.of(pedestrian);
    }

    private void initializeTopography() {
        this.topography = new Topography();
    }

    private void movePedestrianAlongXaxis(Pedestrian pedestrian, int totalFootsteps, double stepSize) {

        for (int i = 0; i < totalFootsteps; i++) {
            double startTime = i;
            double startPosition = i * stepSize;
            double endPosition = startPosition + stepSize;
            FootStep footStep = new FootStep(new VPoint(startPosition, 0), new VPoint(endPosition, 0), startTime, startTime + 1);
            pedestrian.getFootstepHistory().add(footStep);
        }

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

    @Test(expected = IllegalArgumentException.class)
    public void updateThrowsIllegalArgumentExceptionUponUnsupportedStimulus() {
        Stimulus unsupportedStimulus = new Wait();
        pedestrian.setMostImportantStimulus(unsupportedStimulus);

        ThreatCognitionModel modelUnderTest = new ThreatCognitionModel();

        modelUnderTest.update(pedestrians);
    }

    @Test
    public void updateSetsThreatenedIfThreatStimulusAndPedIsNotBlockedByObstacles() {
        initializeTopography();

        ThreatCognitionModel modelUnderTest = new ThreatCognitionModel();
        modelUnderTest.initialize(topography, new Random(0));

        Threat threatStimulus = new Threat();
        pedestrian.setMostImportantStimulus(threatStimulus);
        movePedestrianAlongXaxis(pedestrian, 5, 1);

        modelUnderTest.update(pedestrians);

        assertEquals(SelfCategory.THREATENED, pedestrian.getSelfCategory());
    }

    @Test
    public void updateSetsLatestThreatUnhandledTrueIfNewThreatOcurred() {
        initializeTopography();

        ThreatCognitionModel modelUnderTest = new ThreatCognitionModel();
        modelUnderTest.initialize(topography, new Random(0));

        Threat threatStimulus = new Threat();
        pedestrian.setMostImportantStimulus(threatStimulus);
        movePedestrianAlongXaxis(pedestrian, 5, 1);

        pedestrian.getThreatMemory().setLatestThreatUnhandled(false);

        modelUnderTest.update(pedestrians);

        assertTrue(pedestrian.getThreatMemory().isLatestThreatUnhandled());
    }

    @Test
    public void updateSetsLatestThreatUnhandledFalsIfSameThreatOcurred() {
        initializeTopography();

        ThreatCognitionModel modelUnderTest = new ThreatCognitionModel();
        modelUnderTest.initialize(topography, new Random(0));

        Threat threatStimulus = new Threat();
        pedestrian.setMostImportantStimulus(threatStimulus);
        movePedestrianAlongXaxis(pedestrian, 5, 1);

        for (int i = 0; i < 5; i++) {
            pedestrian.getThreatMemory().setLatestThreatUnhandled(false);
            modelUnderTest.update(pedestrians);

            // First occurrence is a new threat. Subsequent occurrences are no old threats (and must not be handled).
            boolean threatUnhandledExpected = i == 0;

            assertEquals(threatUnhandledExpected, pedestrian.getThreatMemory().isLatestThreatUnhandled());
        }
    }

    @Test
    public void updateAddsThreatToPedestrianThreatMemory() {
        initializeTopography();

        ThreatCognitionModel modelUnderTest = new ThreatCognitionModel();
        modelUnderTest.initialize(topography, new Random(0));

        Threat threatStimulus = new Threat();
        pedestrian.setMostImportantStimulus(threatStimulus);

        assertTrue(pedestrian.getThreatMemory().isEmpty());
        modelUnderTest.update(pedestrians);
        assertFalse(pedestrian.getThreatMemory().isEmpty());

    }

    @Test
    public void updateSetsOutsideThreatAreaIfPedestrianIsBlockedByObstacle() {
        initializeTopography();

        // Place obstacle and pedestrian next to each other at (1, 1)
        AttributesObstacle attributesObstacle = new AttributesObstacle();
        attributesObstacle.setShape(new VRectangle(0, 0, 1, 1));
        Obstacle obstacle = new Obstacle(attributesObstacle);
        topography.addObstacle(obstacle);

        Threat threatStimulus = new Threat();
        pedestrian.setMostImportantStimulus(threatStimulus);
        pedestrian.setPosition(new VPoint(1, 1));
        movePedestrianAlongXaxis(pedestrian, 5, 0.01);
        topography.addElement(pedestrian);

        ThreatCognitionModel modelUnderTest = new ThreatCognitionModel();
        modelUnderTest.initialize(topography, new Random(0));

        modelUnderTest.update(pedestrians);

        assertEquals(SelfCategory.COMMON_FATE, pedestrian.getSelfCategory());
    }

/* Deprecated test.
For version < 2.2, the perception area was defined indirectly via the attributes of the Threat.
For version >=2.2, the perception area is defined outside the stimulus.
The StimulusController filters the stimuli by time, location, and people.
In the ThreatCognitionModel only stimuli arrive that are within range.
It is therefore no longer necessary to check whether the persons are within range.
// no longer necessary
    public void updateSetsThreatenedOrCommonFateUponElapsedTimeStimulusIfPedestrianWasThreatenedBefore() {
        initializeTopography();

        AttributesTarget attributesTarget = new AttributesTarget();
        int threatRadius = 2;
        attributesTarget.setShape(new VCircle(0, 0, threatRadius));
        attributesTarget.setId(1);
        Target targetAsThreat = new Target(attributesTarget);
        topography.addTarget(targetAsThreat);

        Threat threatStimulus = new Threat();
        threatStimulus.setOriginAsTargetId(targetAsThreat.getId());

        threatStimulus.setRadius(threatRadius);
        pedestrian.setMostImportantStimulus(threatStimulus);
        pedestrian.setPosition(new VPoint(0, 0));
        topography.addElement(pedestrian);

        ThreatCognitionModel modelUnderTest = new ThreatCognitionModel();
        modelUnderTest.initialize(topography, new Random(0));

        // The "Threat" stimulus is processed for the first time.
        // Subsequent stimuli will be "ElapsedTime".
        modelUnderTest.update(pedestrians);

        // Let pedestrian run away from threat.
        for (int i = 0; i < 5; i++) {
            pedestrian.setMostImportantStimulus(new ElapsedTime());
            pedestrian.setPosition(new VPoint(i, 0));

            modelUnderTest.update(pedestrians);

            SelfCategory expectedSelfCategory = (i <= threatRadius) ? SelfCategory.THREATENED : SelfCategory.COMMON_FATE;
            assertEquals(expectedSelfCategory, pedestrian.getSelfCategory());
        }
    }*/

    @Test
    public void updateSetsOutGroupMembersToTargetOrientedUponElapsedTimeStimulusIfNoThreatOccurredBefore() {
        initializeTopography();

        pedestrian.setPosition(new VPoint(0, 0));
        pedestrian.setGroupMembership(GroupMembership.OUT_GROUP);
        pedestrian.setMostImportantStimulus(new ElapsedTime());
        topography.addElement(pedestrian);

        ThreatCognitionModel modelUnderTest = new ThreatCognitionModel();
        modelUnderTest.initialize(topography, new Random(0));

        pedestrian.setSelfCategory(SelfCategory.WAIT);
        modelUnderTest.update(pedestrians);

        assertEquals(SelfCategory.TARGET_ORIENTED, pedestrian.getSelfCategory());
    }

    @Test
    public void updateImitatesBehaviorIfThreatenedInGroupNeighborIsPresent() {
        initializeTopography();

        // Set up target representing the threat origin.
        AttributesTarget attributesTarget = new AttributesTarget();
        int threatRadius = 2;
        attributesTarget.setShape(new VCircle(0, 0, threatRadius));
        attributesTarget.setId(1);
        Target targetAsThreat = new Target(attributesTarget);
        topography.addTarget(targetAsThreat);

        Threat threatStimulus = new Threat();
        threatStimulus.setOriginAsTargetId(targetAsThreat.getId());

        //TODO: check test
        //threatStimulus.setRad
        //threatStimulus.setRadius(threatRadius);

        // Set up threatened neighbor.
        AttributesAgent attributesAgent = new AttributesAgent(pedestrian.getId() + 1);
        Pedestrian threatenedNeighbor = new Pedestrian(attributesAgent, new Random());
        threatenedNeighbor.setMostImportantStimulus(threatStimulus);
        threatenedNeighbor.getThreatMemory().add(threatStimulus);
        threatenedNeighbor.setSelfCategory(SelfCategory.COMMON_FATE);
        threatenedNeighbor.setPosition(new VPoint(threatRadius, threatRadius));
        threatenedNeighbor.setGroupMembership(GroupMembership.IN_GROUP);

        // Set up not threatened pedestrian.
        pedestrian.setMostImportantStimulus(new ElapsedTime());
        pedestrian.setPosition(new VPoint(threatRadius + 0.5, threatRadius + 0.5));
        pedestrian.setSelfCategory(SelfCategory.TARGET_ORIENTED);
        pedestrian.setGroupMembership(GroupMembership.IN_GROUP);

        topography.addElement(threatenedNeighbor);
        topography.addElement(pedestrian);

        ThreatCognitionModel modelUnderTest = new ThreatCognitionModel();
        modelUnderTest.initialize(topography, new Random(0));

        modelUnderTest.update(pedestrians);

        assertEquals(SelfCategory.THREATENED, pedestrian.getSelfCategory());
    }

}