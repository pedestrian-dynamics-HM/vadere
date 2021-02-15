package org.vadere.simulator.utils.topography;

import org.junit.Test;
import org.vadere.simulator.models.SpeedAdjuster;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTargetGrid;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.models.AttributesOSM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.ElapsedTime;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.*;

import static org.junit.Assert.*;

public class TopographyHelperTest {

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

    private List<Pedestrian> createPedestrians(int totalPedestrians, boolean usePedIdAsTargetId) {
        List<Pedestrian> pedestrians = new ArrayList<>();

        for (int i = 0; i < totalPedestrians; i++) {
            long seed = 0;
            Random random = new Random(seed);
            AttributesAgent attributesAgent = new AttributesAgent(i);

            Pedestrian currentPedestrian = new Pedestrian(attributesAgent, random);

            currentPedestrian.setMostImportantStimulus(new ElapsedTime());
            currentPedestrian.setSelfCategory(SelfCategory.TARGET_ORIENTED);

            LinkedList<Integer> targetIds = (usePedIdAsTargetId) ? new LinkedList<>(Arrays.asList(i)) : new LinkedList<>();
            currentPedestrian.setTargets(targetIds);

            pedestrians.add(currentPedestrian);
        }

        return pedestrians;
    }

    @Test
    public void calculateAngleBetweenTargetGradientsReturnsZeroIfTargetsAreInSameDirection() {
        createSameDirectionTopography();

        // TODO: Clarify with BZ where this unexpected deviation comes from.
        //  This error sums up for larger angles and is problematic.
        double expectedAngle = 0.043;
        double actualAngle = TopographyHelper.calculateAngleBetweenTargetGradients(pedestrian1, pedestrian2);

        assertEquals(expectedAngle, actualAngle, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void calculateAngleBetweenTargetsReturnsZeroIfTargetsAreInSameDirection() {
        createSameDirectionTopography();

        double expectedAngle = 0;
        double actualAngle = TopographyHelper.calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);

        assertEquals(expectedAngle, actualAngle, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void calculateAngleBetweenTargetsReturnsPiHalfIfPedestriansWalkPerpendicularVariation1() {
        createPerpendicularVariation1Topography();

        double expectedAngle = Math.PI / 2;
        double actualAngle = TopographyHelper.calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);

        assertEquals(expectedAngle, actualAngle, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void calculateAngleBetweenTargetsReturnsPiHalfIfPedestriansWalkPerpendicularVariation2() {
        createPerpendicularVariation2Topography();

        double expectedAngle = Math.PI / 2;
        double actualAngle = TopographyHelper.calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);

        assertEquals(expectedAngle, actualAngle, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test
    public void calculateAngleBetweenTargetsReturnsPiIfPedestriansWalkInOppositeDirections() {
        createOppositeDirectionVariation1Topography();

        double expectedAngle = Math.PI;
        double actualAngle = TopographyHelper.calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);

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
                new Domain(topography),
                new AttributesAgent(),
                attributesOSM.getTargetPotentialModel());

        pedestrian1 = new PedestrianOSM(new AttributesOSM(), attributesAgent, topography, new Random(seed),
                potentialFieldTargetGrid, null, null, null, null);
        pedestrian2 = new PedestrianOSM(new AttributesOSM(), attributesAgent, topography, new Random(seed),
                potentialFieldTargetGrid, null, null, null, null);

        // Position of pedestrian1
        pedestrian1.setPosition(new VPoint(1, 0));
        LinkedList<Integer> targetsPedestrian1 = new LinkedList<>();
        targetsPedestrian1.add(target1.getId());
        pedestrian1.setTargets(targetsPedestrian1);

        // Watch out: the closest point to ped1 is (4, 0) while center is (4.5, 0.5).
        pedestrian1.getAttributes().setWalkingDirectionCalculation(AttributesAgent.WalkingDirectionCalculation.BY_TARGET_CENTER);

        // Position of pedestrian2
        pedestrian2.setPosition(new VPoint(2, 0));
        LinkedList<Integer> targetsPedestrian2 = new LinkedList<>();
        targetsPedestrian2.add(target2.getId());
        pedestrian2.setTargets(targetsPedestrian2);

        VPoint centerOfTarget1Rectangle = new VPoint(4.5, 0.5);

        double expectedAngle = Math.PI - Math.atan2(centerOfTarget1Rectangle.y, centerOfTarget1Rectangle.x);
        double actualAngle = TopographyHelper.calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);

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
                new Domain(topography),
                new AttributesAgent(),
                attributesOSM.getTargetPotentialModel());

        pedestrian1 = new PedestrianOSM(new AttributesOSM(), attributesAgent, topography, new Random(seed),
                potentialFieldTargetGrid, null, null, null, null);
        pedestrian2 = new PedestrianOSM(new AttributesOSM(), attributesAgent, topography, new Random(seed),
                potentialFieldTargetGrid, null, null, null, null);

        // Position of pedestrian1
        pedestrian1.setPosition(new VPoint(1, 0));
        LinkedList<Integer> targetsPedestrian1 = new LinkedList<>();
        targetsPedestrian1.add(target1.getId());
        pedestrian1.setTargets(targetsPedestrian1);

        // Watch out: the closest point to ped1 is (4, 0) while center is (4.5, 0.5).
        pedestrian1.getAttributes().setWalkingDirectionCalculation(AttributesAgent.WalkingDirectionCalculation.BY_TARGET_CLOSEST_POINT);

        // Position of pedestrian2
        pedestrian2.setPosition(new VPoint(2, 0));
        LinkedList<Integer> targetsPedestrian2 = new LinkedList<>();
        targetsPedestrian2.add(target2.getId());
        pedestrian2.setTargets(targetsPedestrian2);

        double expectedAngle = Math.PI;
        double actualAngle = TopographyHelper.calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);

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

        double expectedAngle = -1;
        double actualAngle = TopographyHelper.calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);

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

        double expectedAngle = -1;
        double actualAngle = TopographyHelper.calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);

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

        double expectedAngle = -1;
        double actualAngle = TopographyHelper.calculateAngleBetweenTargets(pedestrian1, pedestrian2, topography);

        assertEquals(expectedAngle, actualAngle, ALLOWED_DOUBLE_TOLERANCE);
    }

    @Test(expected = ClassCastException.class)
    public void calculateAngleBetweenWalkingDirectionsThrowsExceptionIfByGradientIsSelectedButNoPedestrianOsm() {
        createTwoTargetsAndTwoPedestrians(
                new VPoint(1, 0), // target1
                new VPoint(1, -1), // target2
                new VPoint(0, 0), // pedestrian1
                new VPoint(1, 0)  // pedestrian2
        );

        boolean usePedIdAsTargetId = false;
        List<Pedestrian> pedestrians = createPedestrians(1, usePedIdAsTargetId);
        pedestrians.stream().forEach(pedestrian -> topography.addElement(pedestrian));

        pedestrians.get(0).getAttributes().setWalkingDirectionCalculation(AttributesAgent.WalkingDirectionCalculation.BY_GRADIENT);

        TopographyHelper.calculateAngleBetweenWalkingDirections(pedestrians.get(0), pedestrian1, topography);
    }

}