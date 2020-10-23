package org.vadere.simulator.models.osm;

import org.junit.Test;
import org.vadere.simulator.models.SpeedAdjuster;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTargetGrid;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class OSMBehaviorControllerTest {

    private static double ALLOWED_DOUBLE_TOLERANCE = 10e-3;

    private PedestrianOSM pedestrian1;
    private PedestrianOSM pedestrian2;
    private Topography topography;

    private void createSameDirectionTopography() {
        createTwoTargetsAndTwoPedestrians(
                new VPoint(-1, 0), // target1
                new VPoint(-1, 0), // target2
                new VPoint(0, 0), // pedestrian1
                new VPoint(1, 0) // pedestrian2
        );
    }

    private void createPerpendicularVariation1Topography() {
        createTwoTargetsAndTwoPedestrians(
                new VPoint(0, 1), // target1
                new VPoint(-1, 0), // target2
                new VPoint(0, 0), // pedestrian1
                new VPoint(1, 0)  // pedestrian2
        );
    }

    private void createPerpendicularVariation2Topography() {
        createTwoTargetsAndTwoPedestrians(
                new VPoint(1, 0), // target1
                new VPoint(1, -1), // target2
                new VPoint(0, 0), // pedestrian1
                new VPoint(1, 0)  // pedestrian2
        );
    }

    private void createOppositeDirectionVariation1Topography() {
        createTwoTargetsAndTwoPedestrians(
                new VPoint(-1, 0), // target1
                new VPoint(2, 0), // target2
                new VPoint(0, 0), // pedestrian1
                new VPoint(1, 0)  // pedestrian2
        );
    }

    private void createOppositeDirectionVariation2Topography() {
        createTwoTargetsAndTwoPedestrians(
                new VPoint(-1, 0), // target1
                new VPoint(2, 0), // target2
                new VPoint(1, 0), // pedestrian1
                new VPoint(0, 0)  // pedestrian2
        );
    }

    private void createTwoTargetsAndTwoPedestrians(VPoint target1Position, VPoint target2Position, VPoint pedestrian1Position, VPoint pedestrian2Position) {
        List<Target> targets = createTwoTargets(target1Position, target2Position);

        topography = new Topography();
        topography.addTarget(targets.get(0));
        topography.addTarget(targets.get(1));

        List<PedestrianOSM> pedestrians = createTwoPedestrianOSM(pedestrian1Position, pedestrian2Position, topography);
        pedestrian1 = pedestrians.get(0);
        pedestrian2 = pedestrians.get(1);

        topography.addElement(pedestrian1);
        topography.addElement(pedestrian2);
    }

    private List<Target> createTwoTargets(VPoint target1Position, VPoint target2Position) {
        Target target1 = new Target(new AttributesTarget());
        target1.setShape(new VCircle(target1Position, 1));
        target1.getAttributes().setId(1);

        Target target2 = new Target(new AttributesTarget());
        target2.setShape(new VCircle(target2Position, 1));
        target2.getAttributes().setId(2);

        List<Target> targets = new ArrayList<>();
        targets.add(target1);
        targets.add(target2);

        return targets;
    }

    private List<PedestrianOSM> createTwoPedestrianOSM(VPoint pedestrian1Position, VPoint pedestrian2Position, Topography topography) {
        // Create helper objects which are required by a PedestrianOSM:
        AttributesAgent attributesAgent1 = new AttributesAgent(1);
        AttributesAgent attributesAgent2 = new AttributesAgent(2);
        AttributesOSM attributesOSM = new AttributesOSM();
        int seed = 1;

        List<Attributes> floorFieldAttributes = new ArrayList<>();
        floorFieldAttributes.add(new AttributesFloorField());
        IPotentialFieldTargetGrid potentialFieldTargetGrid = IPotentialFieldTargetGrid.createPotentialField(floorFieldAttributes,
                new Domain(topography),
                new AttributesAgent(),
                attributesOSM.getTargetPotentialModel());
        // Force that target potential gets calculated so that the gradient can be used later on.
        double simTimeInSec = 1;
        potentialFieldTargetGrid.preLoop(simTimeInSec);

        List<SpeedAdjuster> noSpeedAdjusters = new ArrayList<>();

        // Create the actual PedestrianOSM
        PedestrianOSM pedestrian1 = new PedestrianOSM(new AttributesOSM(), attributesAgent1, topography, new Random(seed),
                potentialFieldTargetGrid, null, null, noSpeedAdjusters, null);
        PedestrianOSM pedestrian2 = new PedestrianOSM(new AttributesOSM(), attributesAgent2, topography, new Random(seed),
                potentialFieldTargetGrid, null, null, noSpeedAdjusters, null);

        pedestrian1.setPosition(pedestrian1Position);
        LinkedList<Integer> targetsPedestrian1 = new LinkedList<>();
        targetsPedestrian1.add(topography.getTarget(1).getId());
        pedestrian1.setTargets(targetsPedestrian1);
        pedestrian1.setFreeFlowSpeed(1.1);

        pedestrian2.setPosition(new VPoint(pedestrian2Position));
        LinkedList<Integer> targetsPedestrian2 = new LinkedList<>();
        targetsPedestrian2.add(topography.getTarget(2).getId());
        pedestrian2.setTargets(targetsPedestrian2);
        pedestrian2.setFreeFlowSpeed(1.2);

        List<PedestrianOSM> pedestrians = new ArrayList<>();
        pedestrians.add(pedestrian1);
        pedestrians.add(pedestrian2);

        return pedestrians;
    }

    @Test
    public void swapPedestriansSetsNewPositionForBothPedestrians() {
        createSameDirectionTopography();

        VPoint oldPositionPed1 = pedestrian1.getPosition().clone();
        VPoint oldPositionPed2 = pedestrian2.getPosition().clone();
        assertNotEquals(oldPositionPed1, oldPositionPed2);

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();

        double simTimeInSec = 1;
        pedestrian1.setTimeOfNextStep(simTimeInSec);
        pedestrian2.setTimeOfNextStep(simTimeInSec);
        controllerUnderTest.swapPedestrians(pedestrian1, pedestrian2, topography);

        assertEquals(oldPositionPed2, pedestrian1.getPosition());
        assertEquals(oldPositionPed1, pedestrian2.getPosition());
    }

    @Test
    public void swapPedestriansUsesStartTimeOfFirstPedestrianAndMaximumOfBothStepDurations() {
        createSameDirectionTopography();

        double timeOfNextStepPed1 = 1;
        double timeOfNextStepPed2 = 1.5;

        pedestrian1.setTimeOfNextStep(timeOfNextStepPed1);
        pedestrian2.setTimeOfNextStep(timeOfNextStepPed2);
        double maxStepDuration = Math.max(pedestrian1.getDurationNextStep(), pedestrian2.getDurationNextStep());

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();
        controllerUnderTest.swapPedestrians(pedestrian1, pedestrian2, topography);

        double expectedTimeOfNextStep = timeOfNextStepPed1 + maxStepDuration;
        assertEquals(expectedTimeOfNextStep, pedestrian1.getTimeOfNextStep(), ALLOWED_DOUBLE_TOLERANCE);
        assertEquals(expectedTimeOfNextStep, pedestrian2.getTimeOfNextStep(), ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void findSwapCandidateReturnsNullIfGivenPedestrianHasNotTarget() {
        createOppositeDirectionVariation2Topography();

        LinkedList<Integer> noTargets = new LinkedList<>();
        pedestrian1.setTargets(noTargets);

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();
        PedestrianOSM swapCandidate = controllerUnderTest.findSwapCandidate(pedestrian1, topography);

        assertNull(swapCandidate);
    }

    @Test
    public void findSwapCandidateReturnsNullIfNoPedestrianWithinSearchRadius() {
        createOppositeDirectionVariation2Topography();

        double searchRadius = 0.5;
        pedestrian1.getAttributes().setSearchRadius(searchRadius);

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();
        PedestrianOSM swapCandidate = controllerUnderTest.findSwapCandidate(pedestrian1, topography);

        assertNull(swapCandidate);
    }

    @Test
    public void findSwapCandidateReturnsCandidateIfCandidateHasNotTarget() {
        createOppositeDirectionVariation2Topography();

        LinkedList<Integer> noTargets = new LinkedList<>();
        pedestrian2.setTargets(noTargets);

        double searchRadius = 1.5;
        pedestrian1.getAttributes().setSearchRadius(searchRadius);

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();
        PedestrianOSM swapCandidate = controllerUnderTest.findSwapCandidate(pedestrian1, topography);

        assertEquals(pedestrian2.getId(), swapCandidate.getId());
        assertEquals(pedestrian2, swapCandidate);
    }

    @Test
    public void findSwapCandidateReturnsNullIfCandidateIsNotCooperative() {
        createOppositeDirectionVariation2Topography();

        pedestrian2.setSelfCategory(SelfCategory.TARGET_ORIENTED);

        double searchRadius = 1.5;
        pedestrian1.getAttributes().setSearchRadius(searchRadius);

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();
        PedestrianOSM swapCandidate = controllerUnderTest.findSwapCandidate(pedestrian1, topography);

        assertNull(swapCandidate);
    }

    @Test
    public void findSwapCandidateReturnsNullIfGivenPedestrianAndCandidateShareSameWalkingDirection() {
        createOppositeDirectionVariation2Topography();

        pedestrian2.setSelfCategory(SelfCategory.COOPERATIVE);
        pedestrian2.setTargets(pedestrian1.getTargets());

        double searchRadius = 1.5;
        pedestrian1.getAttributes().setSearchRadius(searchRadius);

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();
        PedestrianOSM swapCandidate = controllerUnderTest.findSwapCandidate(pedestrian1, topography);

        assertNull(swapCandidate);
    }

    @Test
    public void findSwapCandidateReturnsCandidateIfCandidateIsCooperativeAndHasDifferentWalkingDirection() {
        createOppositeDirectionVariation2Topography();

        pedestrian2.setSelfCategory(SelfCategory.COOPERATIVE);

        double searchRadius = 1.5;
        pedestrian1.getAttributes().setSearchRadius(searchRadius);

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();
        PedestrianOSM swapCandidate = controllerUnderTest.findSwapCandidate(pedestrian1, topography);

        assertEquals(pedestrian2.getId(), swapCandidate.getId());
        assertEquals(pedestrian2, swapCandidate);
    }

    @Test
    public void findSwapCandidateReturnsCandidateIfWalkingDirectionCalculationIsByGradient() {
        createOppositeDirectionVariation2Topography();

        pedestrian1.getAttributes().setWalkingDirectionCalculation(AttributesAgent.WalkingDirectionCalculation.BY_GRADIENT);
        pedestrian1.getAttributes().setWalkingDirectionSameIfAngleLessOrEqual(45.0);
        pedestrian2.setSelfCategory(SelfCategory.COOPERATIVE);

        double searchRadius = 1.5;
        pedestrian1.getAttributes().setSearchRadius(searchRadius);

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();
        PedestrianOSM swapCandidate = controllerUnderTest.findSwapCandidate(pedestrian1, topography);

        assertEquals(pedestrian2.getId(), swapCandidate.getId());
        assertEquals(pedestrian2, swapCandidate);
    }

    @Test
    public void findSwapCandidateReturnsCandidateIfWalkingDirectionCalculationIsByTargetCenter() {
        createOppositeDirectionVariation2Topography();

        pedestrian1.getAttributes().setWalkingDirectionCalculation(AttributesAgent.WalkingDirectionCalculation.BY_TARGET_CENTER);
        pedestrian1.getAttributes().setWalkingDirectionSameIfAngleLessOrEqual(45.0);
        pedestrian2.setSelfCategory(SelfCategory.COOPERATIVE);

        double searchRadius = 1.5;
        pedestrian1.getAttributes().setSearchRadius(searchRadius);

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();
        PedestrianOSM swapCandidate = controllerUnderTest.findSwapCandidate(pedestrian1, topography);

        assertEquals(pedestrian2.getId(), swapCandidate.getId());
        assertEquals(pedestrian2, swapCandidate);
    }

    @Test
    public void findSwapCandidateReturnsCandidateIfWalkingDirectionCalculationIsByTargetClosestPoint() {
        createOppositeDirectionVariation2Topography();

        pedestrian1.getAttributes().setWalkingDirectionCalculation(AttributesAgent.WalkingDirectionCalculation.BY_TARGET_CLOSEST_POINT);
        pedestrian1.getAttributes().setWalkingDirectionSameIfAngleLessOrEqual(45.0);
        pedestrian2.setSelfCategory(SelfCategory.COOPERATIVE);

        double searchRadius = 1.5;
        pedestrian1.getAttributes().setSearchRadius(searchRadius);

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();
        PedestrianOSM swapCandidate = controllerUnderTest.findSwapCandidate(pedestrian1, topography);

        assertEquals(pedestrian2.getId(), swapCandidate.getId());
        assertEquals(pedestrian2, swapCandidate);
    }

    @Test
    public void findSwapCandidateReturnsNullCandidateIfParameterWalkingDirectionSameIfAngleIsTooLarge() {
        createOppositeDirectionVariation2Topography();

        pedestrian1.getAttributes().setWalkingDirectionCalculation(AttributesAgent.WalkingDirectionCalculation.BY_GRADIENT);
        pedestrian1.getAttributes().setWalkingDirectionSameIfAngleLessOrEqual(270.0);
        pedestrian2.setSelfCategory(SelfCategory.COOPERATIVE);

        double searchRadius = 1.5;
        pedestrian1.getAttributes().setSearchRadius(searchRadius);

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();
        PedestrianOSM swapCandidate = controllerUnderTest.findSwapCandidate(pedestrian1, topography);

        assertNull(swapCandidate);
    }

    @Test(expected = IllegalArgumentException.class)
    public void findSwapCandidateUsesAttributesFromFirstPedestrian() {
        createOppositeDirectionVariation2Topography();

        pedestrian2.getAttributes().setWalkingDirectionCalculation(AttributesAgent.WalkingDirectionCalculation.BY_GRADIENT);
        pedestrian2.getAttributes().setWalkingDirectionSameIfAngleLessOrEqual(270.0);
        pedestrian2.setSelfCategory(SelfCategory.COOPERATIVE);

        double searchRadius = 1.5;
        pedestrian1.getAttributes().setSearchRadius(searchRadius);

        // Exception expected because ped1 requests calculation type "BY_TARGET_CENTER",
        // but ped2 requests "BY_GRADIENT" which contradicts.
        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();
        PedestrianOSM swapCandidate = controllerUnderTest.findSwapCandidate(pedestrian1, topography);
    }

    @Test
    public void waitSetsTimeOfNextStepForEventDrivenUpdateScheme() {
        createSameDirectionTopography();

        double currentSimTimeInSec = 1.0;
        double timeOfNextStep = 0.5;

        pedestrian1.setTimeOfNextStep(timeOfNextStep);
        assertEquals(timeOfNextStep, pedestrian1.getTimeOfNextStep(), ALLOWED_DOUBLE_TOLERANCE);

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();
        controllerUnderTest.wait(pedestrian1, topography, currentSimTimeInSec);

        double expectedTimeOfNextStep = currentSimTimeInSec + timeOfNextStep;
        assertEquals(expectedTimeOfNextStep, pedestrian1.getTimeOfNextStep(), ALLOWED_DOUBLE_TOLERANCE);
    }

}