package org.vadere.simulator.models.osm;

import org.junit.Test;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTargetGrid;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.simulator.utils.cache.ScenarioCache;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class OSMBehaviorControllerTest {

    private static double ALLOWED_DOUBLE_TOLERANCE = 10e-3;

    private PedestrianOSM pedestrian1;
    private PedestrianOSM pedestrian2;
    private Topography topography;

    private void createTwoTargetsAndTwoPedestrians(VPoint target1Position, VPoint target2Position, VPoint pedestrian1Position, VPoint pedestrian2Position) {
        // Create a topography with two targets.
        topography = new Topography();

        Target target1 = new Target(new AttributesTarget());
        target1.setShape(new VCircle(target1Position, 1));
        target1.getAttributes().setId(1);

        Target target2 = new Target(new AttributesTarget());
        target2.setShape(new VCircle(target2Position, 1));
        target2.getAttributes().setId(2);

        topography.addTarget(target1);
        topography.addTarget(target2);

        // Create two pedestrians and assign them the two targets from above.
        AttributesAgent attributesAgent = new AttributesAgent();
        AttributesOSM attributesOSM = new AttributesOSM();
        int seed = 1;

        List<Attributes> floorFieldAttributes = new ArrayList<>();
        floorFieldAttributes.add(new AttributesFloorField());
        IPotentialFieldTargetGrid potentialFieldTargetGrid = IPotentialFieldTargetGrid.createPotentialField(floorFieldAttributes,
                topography,
                new AttributesAgent(),
                attributesOSM.getTargetPotentialModel(),
                ScenarioCache.empty());

        pedestrian1 = new PedestrianOSM(new AttributesOSM(), attributesAgent, topography, new Random(1),
                potentialFieldTargetGrid, null, null, null, null);
        pedestrian2 = new PedestrianOSM(new AttributesOSM(), attributesAgent, topography, new Random(1),
                potentialFieldTargetGrid, null, null, null, null);

        pedestrian1.setPosition(pedestrian1Position);
        LinkedList<Integer> targetsPedestrian1 = new LinkedList<>();
        targetsPedestrian1.add(target1.getId());
        pedestrian1.setTargets(targetsPedestrian1);

        pedestrian2.setPosition(new VPoint(pedestrian2Position));
        LinkedList<Integer> targetsPedestrian2 = new LinkedList<>();
        targetsPedestrian2.add(target2.getId());
        pedestrian2.setTargets(targetsPedestrian2);
    }

    @Test
    public void calculateAngleBetweenTargetsReturnsCorrectResultIfTargetAndPedestriansAreVerticalZeroQuarterCircle() {
        createTwoTargetsAndTwoPedestrians(
                new VPoint(-1, 0), // target1
                new VPoint(-1, 0), // target2
                new VPoint(0, 0), // pedestrian1
                new VPoint(1, 0) // pedestrian2
        );

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();

        double expectedAngle = 0;
        double actualAngle = controllerUnderTest.calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);

        assertEquals(expectedAngle, actualAngle, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void calculateAngleBetweenTargetsReturnsCorrectResultIfTargetAndPedestriansAreVerticalOneQuarterCircle() {
        createTwoTargetsAndTwoPedestrians(
                new VPoint(0, 1), // target1
                new VPoint(-1, 0), // target2
                new VPoint(0, 0), // pedestrian1
                new VPoint(1, 0)  // pedestrian2
        );

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();

        double expectedAngle = Math.PI / 2;
        double actualAngle = controllerUnderTest.calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);

        assertEquals(expectedAngle, actualAngle, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void calculateAngleBetweenTargetsReturnsCorrectResultIfTargetAndPedestriansAreVerticalTwoQuarterCircle() {
        createTwoTargetsAndTwoPedestrians(
                new VPoint(-1, 0), // target1
                new VPoint(2, 0), // target2
                new VPoint(0, 0), // pedestrian1
                new VPoint(1, 0)  // pedestrian2
        );

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();

        double expectedAngle = Math.PI;
        double actualAngle = controllerUnderTest.calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);

        assertEquals(expectedAngle, actualAngle, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void calculateAngleBetweenTargetsReturnsCorrectResultIfTargetAndPedestriansAreVerticalThreeQuarterCircle() {
        createTwoTargetsAndTwoPedestrians(
                new VPoint(1, 0), // target1
                new VPoint(1, -1), // target2
                new VPoint(0, 0), // pedestrian1
                new VPoint(1, 0)  // pedestrian2
        );

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();

        double expectedAngle = Math.PI / 2;
        double actualAngle = controllerUnderTest.calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);

        assertEquals(expectedAngle, actualAngle, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void calculateAngleBetweenTargetsReturnsCorrectResultIfPedestrian1UsesAngleCalculationTypeUseCenter() {
        // Create a topography with two targets.
        topography = new Topography();

        // Position of target1
        Target target1 = new Target(new AttributesTarget());
        target1.setShape(new VRectangle(4, 0, 1, 1));
        target1.getAttributes().setId(1);

        // Position of target2
        Target target2 = new Target(new AttributesTarget());
        target2.setShape(new VCircle(0, 0, 1));
        target2.getAttributes().setId(2);

        topography.addTarget(target1);
        topography.addTarget(target2);

        // Create two pedestrians and assign them the two targets from above.
        AttributesAgent attributesAgent = new AttributesAgent();
        AttributesOSM attributesOSM = new AttributesOSM();
        int seed = 1;

        List<Attributes> floorFieldAttributes = new ArrayList<>();
        floorFieldAttributes.add(new AttributesFloorField());
        IPotentialFieldTargetGrid potentialFieldTargetGrid = IPotentialFieldTargetGrid.createPotentialField(floorFieldAttributes,
                topography,
                new AttributesAgent(),
                attributesOSM.getTargetPotentialModel(),
                ScenarioCache.empty());

        pedestrian1 = new PedestrianOSM(new AttributesOSM(), attributesAgent, topography, new Random(1),
                potentialFieldTargetGrid, null, null, null, null);
        pedestrian2 = new PedestrianOSM(new AttributesOSM(), attributesAgent, topography, new Random(1),
                potentialFieldTargetGrid, null, null, null, null);

        // Position of pedestrian1
        pedestrian1.setPosition(new VPoint(1, 0));
        LinkedList<Integer> targetsPedestrian1 = new LinkedList<>();
        targetsPedestrian1.add(target1.getId());
        pedestrian1.setTargets(targetsPedestrian1);

        // Watch out: the closest point to ped1 is (4, 0) while center is (4.5, 0.5).
        pedestrian1.getAttributes().setAngleCalculationType(AttributesAgent.AngleCalculationType.USE_CENTER);

        // Position of pedestrian2
        pedestrian2.setPosition(new VPoint(2, 0));
        LinkedList<Integer> targetsPedestrian2 = new LinkedList<>();
        targetsPedestrian2.add(target2.getId());
        pedestrian2.setTargets(targetsPedestrian2);

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();

        VPoint centerOfTarget1Rectangle = new VPoint(4.5, 0.5);

        double expectedAngle = Math.PI - Math.atan2(centerOfTarget1Rectangle.y, centerOfTarget1Rectangle.x);
        double actualAngle = controllerUnderTest.calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);

        // It seems that "Math.atan2()" is not as precise as expected. Therefore, increase the allowed tolerance.
        assertEquals(expectedAngle, actualAngle, 10e-1);
    }

    @Test
    public void calculateAngleBetweenTargetsReturnsCorrectResultIfPedestrian1UsesAngleCalculationTypeUseClosestPoint() {
        // Create a topography with two targets.
        topography = new Topography();

        // Position of target1
        Target target1 = new Target(new AttributesTarget());
        target1.setShape(new VRectangle(4, 0, 1, 1));
        target1.getAttributes().setId(1);

        // Position of target2
        Target target2 = new Target(new AttributesTarget());
        target2.setShape(new VCircle(0, 0, 1));
        target2.getAttributes().setId(2);

        topography.addTarget(target1);
        topography.addTarget(target2);

        // Create two pedestrians and assign them the two targets from above.
        AttributesAgent attributesAgent = new AttributesAgent();
        AttributesOSM attributesOSM = new AttributesOSM();
        int seed = 1;

        List<Attributes> floorFieldAttributes = new ArrayList<>();
        floorFieldAttributes.add(new AttributesFloorField());
        IPotentialFieldTargetGrid potentialFieldTargetGrid = IPotentialFieldTargetGrid.createPotentialField(floorFieldAttributes,
                topography,
                new AttributesAgent(),
                attributesOSM.getTargetPotentialModel(),
                ScenarioCache.empty());

        pedestrian1 = new PedestrianOSM(new AttributesOSM(), attributesAgent, topography, new Random(1),
                potentialFieldTargetGrid, null, null, null, null);
        pedestrian2 = new PedestrianOSM(new AttributesOSM(), attributesAgent, topography, new Random(1),
                potentialFieldTargetGrid, null, null, null, null);

        // Position of pedestrian1
        pedestrian1.setPosition(new VPoint(1, 0));
        LinkedList<Integer> targetsPedestrian1 = new LinkedList<>();
        targetsPedestrian1.add(target1.getId());
        pedestrian1.setTargets(targetsPedestrian1);

        // Watch out: the closest point to ped1 is (4, 0) while center is (4.5, 0.5).
        pedestrian1.getAttributes().setAngleCalculationType(AttributesAgent.AngleCalculationType.USE_CLOSEST_POINT);

        // Position of pedestrian2
        pedestrian2.setPosition(new VPoint(2, 0));
        LinkedList<Integer> targetsPedestrian2 = new LinkedList<>();
        targetsPedestrian2.add(target2.getId());
        pedestrian2.setTargets(targetsPedestrian2);

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();

        double expectedAngle = Math.PI;
        double actualAngle = controllerUnderTest.calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);

        assertEquals(expectedAngle, actualAngle, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void calculateAngleBetweenTargetsReturnsMinusOneIfFirstPedestrianHasNoTarget() {
        createTwoTargetsAndTwoPedestrians(
                new VPoint(1, 0), // target1
                new VPoint(1, -1), // target2
                new VPoint(0, 0), // pedestrian1
                new VPoint(1, 0)  // pedestrian2
        );

        pedestrian1.setTargets(new LinkedList<>());

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();

        double expectedAngle = -1;
        double actualAngle = controllerUnderTest.calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);

        assertEquals(expectedAngle, actualAngle, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void calculateAngleBetweenTargetsReturnsMinusOneIfSecondPedestrianHasNoTarget() {
        createTwoTargetsAndTwoPedestrians(
                new VPoint(1, 0), // target1
                new VPoint(1, -1), // target2
                new VPoint(0, 0), // pedestrian1
                new VPoint(1, 0)  // pedestrian2
        );

        pedestrian2.setTargets(new LinkedList<>());

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();

        double expectedAngle = -1;
        double actualAngle = controllerUnderTest.calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);

        assertEquals(expectedAngle, actualAngle, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void calculateAngleBetweenTargetsReturnsMinusOneIfBothPedestriansHaveNoTargets() {
        createTwoTargetsAndTwoPedestrians(
                new VPoint(1, 0), // target1
                new VPoint(1, -1), // target2
                new VPoint(0, 0), // pedestrian1
                new VPoint(1, 0)  // pedestrian2
        );

        pedestrian1.setTargets(new LinkedList<>());
        pedestrian2.setTargets(new LinkedList<>());

        OSMBehaviorController controllerUnderTest = new OSMBehaviorController();

        double expectedAngle = -1;
        double actualAngle = controllerUnderTest.calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);

        assertEquals(expectedAngle, actualAngle, ALLOWED_DOUBLE_TOLERANCE);
    }

}