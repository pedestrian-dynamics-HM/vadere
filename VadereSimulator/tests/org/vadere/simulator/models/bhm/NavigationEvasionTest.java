package org.vadere.simulator.models.bhm;

import org.junit.Test;
import org.vadere.simulator.models.bhm.helpers.navigation.NavigationEvasion;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTargetGrid;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesBHM;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class NavigationEvasionTest {

    // Static Variables
    public static double ALLOWED_DOUBLE_ERROR = 10e-2;

    // Member Variables
    private Topography topography;
    private PedestrianBHM pedestrian1;
    private PedestrianBHM pedestrian2;

    // Helper Methods (to create topography)
    private void createOppositeDirectionVariation1Topography() {
        createTwoTargetsAndTwoPedestrians(
                new VPoint(-1, 0), // target1
                new VPoint(2, 0), // target2
                new VPoint(0, 0), // pedestrian1
                new VPoint(1, 0)  // pedestrian2
        );
    }

    private void createTwoTargetsAndTwoPedestrians(VPoint target1Position, VPoint target2Position, VPoint pedestrian1Position, VPoint pedestrian2Position) {
        List<Target> targets = createTwoTargets(target1Position, target2Position);

        topography = new Topography();
        topography.addTarget(targets.get(0));
        topography.addTarget(targets.get(1));

        int startPedestrianId = 1;
        List<PedestrianBHM> pedestrians = createTwoPedestrianBHM(pedestrian1Position, pedestrian2Position, startPedestrianId, topography);
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

    private List<PedestrianBHM> createTwoPedestrianBHM(VPoint pedestrian1Position, VPoint pedestrian2Position, int startId, Topography topography) {
        // Create helper objects which are required by a PedestrianOSM:
        AttributesAgent attributesAgent1 = new AttributesAgent(startId);
        AttributesAgent attributesAgent2 = new AttributesAgent(startId + 1);
        AttributesBHM attributesBHM = new AttributesBHM();
        int seed = 1;

        List<Attributes> floorFieldAttributes = new ArrayList<>();
        floorFieldAttributes.add(new AttributesFloorField());
        IPotentialFieldTargetGrid potentialFieldTargetGrid = IPotentialFieldTargetGrid.createPotentialField(floorFieldAttributes,
                new Domain(topography),
                new AttributesAgent(),
                "org.vadere.simulator.models.potential.fields.PotentialFieldTargetGrid");
        // Force that target potential gets calculated so that the gradient can be used later on.
        double simTimeInSec = 1;
        potentialFieldTargetGrid.preLoop(simTimeInSec);

        List<SpeedAdjuster> noSpeedAdjusters = new ArrayList<>();

        // Create the actual PedestrianOSM
        PedestrianBHM pedestrian1 = new PedestrianBHM(topography,attributesAgent1, attributesBHM, new Random(seed), potentialFieldTargetGrid);
        PedestrianBHM pedestrian2 = new PedestrianBHM(topography,attributesAgent2, attributesBHM, new Random(seed), potentialFieldTargetGrid);

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

        List<PedestrianBHM> pedestrians = new ArrayList<>();
        pedestrians.add(pedestrian1);
        pedestrians.add(pedestrian2);

        return pedestrians;
    }

    // Tests
    @Test
    public void getCurrentTargetDirectionNormedByGradientReturnsNormedGradient() {
        createOppositeDirectionVariation1Topography();

        NavigationEvasion navigationUnderTest = new NavigationEvasion();
        navigationUnderTest.initialize(pedestrian1, topography, null);

        // Ped1 walks from (0,0) to (-1,0) => target gradient is (1, 0) => walking direction (gradient) is roughly (-1, 0).
        VPoint nextWalkingDirection = navigationUnderTest.getCurrentTargetDirectionNormedByGradient();

        assertEquals(-1, nextWalkingDirection.x, ALLOWED_DOUBLE_ERROR);
        assertEquals(0, nextWalkingDirection.y, ALLOWED_DOUBLE_ERROR);
    }

    @Test
    public void getNextPositionUsesCurrentPedestrianPosition() {
        createOppositeDirectionVariation1Topography();

        NavigationEvasion navigationUnderTest = new NavigationEvasion();
        navigationUnderTest.initialize(pedestrian1, topography, null);

        // Ped1 stands at (0,0).
        VPoint currentWalkingDirection = new VPoint(0, 0);
        double rotationAngleInDegCcw = 0;
        double stepLength = 0;

        VPoint oldPosition = pedestrian1.getPosition();
        VPoint nextPosition = navigationUnderTest.getNextPosition(currentWalkingDirection, rotationAngleInDegCcw, stepLength);

        assertEquals(oldPosition.x, nextPosition.x, ALLOWED_DOUBLE_ERROR);
        assertEquals(oldPosition.y, nextPosition.y, ALLOWED_DOUBLE_ERROR);
    }

    @Test
    public void getNextPositionUsesCurrentWalkingDirection() {
        createOppositeDirectionVariation1Topography();

        NavigationEvasion navigationUnderTest = new NavigationEvasion();
        navigationUnderTest.initialize(pedestrian1, topography, null);

        // Ped1 stands at (0,0).
        VPoint currentWalkingDirection = new VPoint(1, 2);
        double rotationAngleInDegCcw = 0;
        double stepLength = 1;

        VPoint oldPosition = pedestrian1.getPosition();
        VPoint nextPosition = navigationUnderTest.getNextPosition(currentWalkingDirection, rotationAngleInDegCcw, stepLength);

        double xExpectedAfterRotation = oldPosition.x + currentWalkingDirection.x;
        double yExpectedAfterRotation = oldPosition.y + currentWalkingDirection.y;
        assertEquals(xExpectedAfterRotation, nextPosition.x, ALLOWED_DOUBLE_ERROR);
        assertEquals(yExpectedAfterRotation, nextPosition.y, ALLOWED_DOUBLE_ERROR);
    }

    @Test
    public void getNextPositionUsesCurrentGivenStepLength() {
        createOppositeDirectionVariation1Topography();

        NavigationEvasion navigationUnderTest = new NavigationEvasion();
        navigationUnderTest.initialize(pedestrian1, topography, null);

        // Ped1 stands at (0,0).
        VPoint currentWalkingDirection = new VPoint(1, 1);
        double rotationAngleInDegCcw = 0;
        double stepLength = 2;

        VPoint oldPosition = pedestrian1.getPosition();
        VPoint nextPosition = navigationUnderTest.getNextPosition(currentWalkingDirection, rotationAngleInDegCcw, stepLength);

        double xExpectedAfterRotation = oldPosition.x + (stepLength * currentWalkingDirection.x);
        double yExpectedAfterRotation = oldPosition.y + (stepLength * currentWalkingDirection.y);
        assertEquals(xExpectedAfterRotation, nextPosition.x, ALLOWED_DOUBLE_ERROR);
        assertEquals(yExpectedAfterRotation, nextPosition.y, ALLOWED_DOUBLE_ERROR);
    }

    @Test
    public void getNextPositionRotatesWalkingDirectionCounterClockwiseByDefault() {
        createOppositeDirectionVariation1Topography();

        NavigationEvasion navigationUnderTest = new NavigationEvasion();
        navigationUnderTest.initialize(pedestrian1, topography, null);

        VPoint currentWalkingDirection = new VPoint(1, 0);
        double rotationAngleInDegCcw = 90;
        double stepLength = 1;

        VPoint oldPosition = pedestrian1.getPosition();
        VPoint nextPosition = navigationUnderTest.getNextPosition(currentWalkingDirection, rotationAngleInDegCcw, stepLength);

        // Ped1 stands at (0,0).
        double xExpectedAfterRotation = oldPosition.x + currentWalkingDirection.y;
        double yExpectedAfterRotation = oldPosition.y + currentWalkingDirection.x;
        assertEquals(xExpectedAfterRotation, nextPosition.x, ALLOWED_DOUBLE_ERROR);
        assertEquals(yExpectedAfterRotation, nextPosition.y, ALLOWED_DOUBLE_ERROR);
    }

    @Test
    public void getNextPositionRotatesClockwiseIfAngleIsNegative() {
        createOppositeDirectionVariation1Topography();

        NavigationEvasion navigationUnderTest = new NavigationEvasion();
        navigationUnderTest.initialize(pedestrian1, topography, null);

        VPoint currentWalkingDirection = new VPoint(1, 0);
        double rotationAngleInDegCcw = -90;
        double stepLength = 1;

        VPoint oldPosition = pedestrian1.getPosition();
        VPoint nextPosition = navigationUnderTest.getNextPosition(currentWalkingDirection, rotationAngleInDegCcw, stepLength);

        // Ped1 stands at (0,0).
        double xExpectedAfterRotation = oldPosition.x + currentWalkingDirection.y;
        double yExpectedAfterRotation = oldPosition.y - currentWalkingDirection.x;
        assertEquals(xExpectedAfterRotation, nextPosition.x, ALLOWED_DOUBLE_ERROR);
        assertEquals(yExpectedAfterRotation, nextPosition.y, ALLOWED_DOUBLE_ERROR);
    }

    @Test
    public void getNextPositionRotates180DegCorrectly() {
        createOppositeDirectionVariation1Topography();

        NavigationEvasion navigationUnderTest = new NavigationEvasion();
        navigationUnderTest.initialize(pedestrian1, topography, null);

        VPoint currentWalkingDirection = new VPoint(1, 0);
        double rotationAngleInDegCcw = 180;
        double stepLength = 1;

        VPoint nextPosition = navigationUnderTest.getNextPosition(currentWalkingDirection, rotationAngleInDegCcw, stepLength);

        // Ped1 stands at (0,0).
        double xExpectedAfterRotation = -1;
        double yExpectedAfterRotation = 0;
        assertEquals(xExpectedAfterRotation, nextPosition.x, ALLOWED_DOUBLE_ERROR);
        assertEquals(yExpectedAfterRotation, nextPosition.y, ALLOWED_DOUBLE_ERROR);
    }

    @Test
    public void getNavigationPositionReturnsTangentialPositionFullSpeedIfNotOccupied() {
        createOppositeDirectionVariation1Topography();

        NavigationEvasion navigationUnderTest = new NavigationEvasion();
        navigationUnderTest.initialize(pedestrian1, topography, null);

        VPoint newPosition = navigationUnderTest.getNavigationPosition();

        VPoint oldPosition = pedestrian1.getPosition();
        VPoint walkingDirection = new VPoint(-1 ,0);
        VPoint expectedTangentialPosition = oldPosition.add(walkingDirection.norm().rotate(Math.toRadians(-45)).scalarMultiply(pedestrian1.getStepLength()));

        assertEquals(expectedTangentialPosition.x, newPosition.x, ALLOWED_DOUBLE_ERROR);
        assertEquals(expectedTangentialPosition.y, newPosition.y, ALLOWED_DOUBLE_ERROR);
    }

    @Test
    public void getNavigationPositionReturnsTangentialPositionHalfSpeedIfFullSpeedOccupied() {
        createOppositeDirectionVariation1Topography();

        // Occupy tangential/full speed position
        VPoint oldPosition = pedestrian1.getPosition();
        VPoint walkingDirection = new VPoint(-1 ,0);

        VPoint tangentialPosition = oldPosition.add(walkingDirection.norm().rotate(Math.toRadians(-45)).scalarMultiply(pedestrian1.getStepLength()));

        int startPedestrianId = 3;
        List<PedestrianBHM> pedestrians = createTwoPedestrianBHM(tangentialPosition, tangentialPosition, startPedestrianId, topography);
        pedestrians.stream().forEach(pedestrian -> topography.addElement(pedestrian));

        NavigationEvasion navigationUnderTest = new NavigationEvasion();
        navigationUnderTest.initialize(pedestrian1, topography, null);

        VPoint newPosition = navigationUnderTest.getNavigationPosition();

        VPoint expectedTangentialHalfSpeedPosition = oldPosition.add(walkingDirection.norm().rotate(Math.toRadians(-45)).scalarMultiply(pedestrian1.getStepLength() * 0.5));

        assertEquals(expectedTangentialHalfSpeedPosition.x, newPosition.x, ALLOWED_DOUBLE_ERROR);
        assertEquals(expectedTangentialHalfSpeedPosition.y, newPosition.y, ALLOWED_DOUBLE_ERROR);
    }

    @Test
    public void getNavigationPositionReturnsSidestepPositionHalfSpeedIfTangentialOccupied() {
        createOppositeDirectionVariation1Topography();

        // Occupy tangential full and half speed position
        VPoint oldPosition = pedestrian1.getPosition();
        VPoint walkingDirection = new VPoint(-1 ,0);

        VPoint tangentialPositionFullSpeed = oldPosition.add(walkingDirection.norm().rotate(Math.toRadians(-45)).scalarMultiply(pedestrian1.getStepLength()));
        VPoint tangentialPositionHalfSpeed = oldPosition.add(walkingDirection.norm().rotate(Math.toRadians(-45)).scalarMultiply(pedestrian1.getStepLength() * 0.5));

        int startPedestrianId = 3;
        List<PedestrianBHM> pedestrians = createTwoPedestrianBHM(tangentialPositionFullSpeed, tangentialPositionHalfSpeed, startPedestrianId, topography);
        pedestrians.stream().forEach(pedestrian -> topography.addElement(pedestrian));

        NavigationEvasion navigationUnderTest = new NavigationEvasion();
        navigationUnderTest.initialize(pedestrian1, topography, null);

        VPoint newPosition = navigationUnderTest.getNavigationPosition();

        VPoint expectedTangentialHalfSpeedPosition = oldPosition.add(walkingDirection.norm().rotate(Math.toRadians(-90)).scalarMultiply(pedestrian1.getStepLength() * 0.5));

        assertEquals(expectedTangentialHalfSpeedPosition.x, newPosition.x, ALLOWED_DOUBLE_ERROR);
        assertEquals(expectedTangentialHalfSpeedPosition.y, newPosition.y, ALLOWED_DOUBLE_ERROR);
    }

}