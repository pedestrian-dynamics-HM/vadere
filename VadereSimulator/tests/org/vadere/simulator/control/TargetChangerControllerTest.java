package org.vadere.simulator.control;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.control.scenarioelements.TargetChangerController;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.attributes.scenario.AttributesTargetChanger;
import org.vadere.state.scenario.*;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TargetChangerControllerTest {

    /**
     * The topography for this test contains
     * - 2 pedestrians (P1 and P2)
     * - 2 targets (T1 and T2)
     * - 1 target changer (TC1)
     * and looks like this:
     *
     * <pre>
     * +----------------------+
     * |        +----+ +----+ |
     * |        |    | |    | |
     * |        |    | | T2 | |
     * |        |    | +----+ |
     * |        |    |        |
     * |        |TC1 |        | 10 meters
     * |        |    |        |
     * |        |    |        |
     * |        |    | +----+ |
     * |        | P2 | |    | |
     * | P1     | *  | | T1 | |
     * | *      +----+ +----+ |
     * +----------------------+
     *        10 meters
     * </pre>
     */
    private Topography topography;
    private List<Pedestrian> pedestrians;
    private List<Target> targets;
    double simTimeInSec = 0;
    // The "TargetChanger" is added by each test individually
    // to meet the requirements of the test.

    @Before
    public void setUp() throws Exception {
        topography = new Topography();
        pedestrians = createTwoPedestrianWithTargetT1(1);
        targets = createTwoTargets();
        simTimeInSec = 0;

        for (Pedestrian pedestrian : pedestrians) {
            topography.addElement(pedestrian);
        }
        for (Target target : targets) {
            topography.addTarget(target);
        }
    }

    private LinkedList<Integer> createIntegerList(Integer... integers){
        LinkedList<Integer> integerList = new LinkedList<>();
        for (Integer integer : integers) {
            integerList.add(integer);
        }
        return integerList;
    }

    private List<Pedestrian> createTwoPedestrianWithTargetT1(int startId) {
        int seed = 0;
        Random random = new Random(seed);

        LinkedList<Integer> targetsPed1 = new LinkedList<>();
        targetsPed1.add(1);
        LinkedList<Integer> targetsPed2 = new LinkedList<>();
        targetsPed2.add(1);

        Pedestrian pedestrian1 = new Pedestrian(new AttributesAgent(startId), random);
        pedestrian1.setPosition(new VPoint(1, 1));
        pedestrian1.setTargets(targetsPed1);

        Pedestrian pedestrian2 = new Pedestrian(new AttributesAgent(startId +  1), random);
        pedestrian2.setPosition(new VPoint(5, 2));
        pedestrian2.setTargets(targetsPed2);

        // Watch out: Use an "ArrayList" to keep order and
        // index 0 refers to pedestrian p1!
        List<Pedestrian> pedestrians = new ArrayList<>();
        pedestrians.add(pedestrian1);
        pedestrians.add(pedestrian2);

        return pedestrians;
    }

    private List<Target> createTwoTargets() {
        boolean absorbing = true;
        AttributesTarget attributesTarget1 = new AttributesTarget(new VRectangle(7, 1, 2, 2), 1, absorbing);
        AttributesTarget attributesTarget2 = new AttributesTarget(new VRectangle(7, 7, 2, 2), 2, absorbing);

        Target target1 = new Target(attributesTarget1);
        Target target2 = new Target(attributesTarget2);

        List<Target> targets = new ArrayList<>();
        targets.add(target1);
        targets.add(target2);

        return targets;
    }

    private AttributesTargetChanger createAttributesWithFixedRectangle() {
        return new AttributesTargetChanger(new VRectangle(4, 1, 2, 8), 1);
    }

    private TargetChangerController createTargetChangerController(TargetChanger targetChanger) {
        int seed = 0;
        Random random = new Random(seed);

        return new TargetChangerController(topography, targetChanger, random);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getProcessedAgentsReturnsEmptyMapIfUpdateWasNotInvoked() {
        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);

        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        Map<Integer, Agent> processedAgents = controllerUnderTest.getProcessedAgents();
        assertTrue(processedAgents.isEmpty());
    }

    @Test
    public void updateProcessesOnlyAgentsWithinTargetChanger() {
        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);

        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        controllerUnderTest.update(simTimeInSec);

        Map<Integer, Agent> processedAgents = controllerUnderTest.getProcessedAgents();
        Agent processedAgent = processedAgents.get(pedestrians.get(1).getId());

        assertEquals(1, processedAgents.size());
        assertEquals(pedestrians.get(1).getId(), processedAgent.getId());
    }

    @Test
    public void updateChangesTargetListOfAffectedPedestrianIfProbabilityIsOne() {
        LinkedList<Integer> nextTarget = createIntegerList(2);

        LinkedList<Double> probability = new LinkedList<Double>(Arrays.asList(1.0)); // must be 1

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(nextTarget);

        attributesTargetChanger.setProbabilitiesToChangeTarget(probability);

        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        int expectedTargetId = 1;
        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), expectedTargetId);
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), expectedTargetId);

        controllerUnderTest.update(simTimeInSec);

        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), expectedTargetId);
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), nextTarget);
    }

    @Test
    public void updateSetsNextTargetListIndexToZeroOfAffectedPedestrianIfProbabilityIsOne() {
        LinkedList<Integer> currentTargets = new LinkedList<Integer>(Arrays.asList(1, 2, 3));
        int nextTargetIndex = 1;

        pedestrians.get(1).setTargets(currentTargets);
        pedestrians.get(1).setNextTargetListIndex(nextTargetIndex);

        LinkedList<Integer> nextTarget = createIntegerList(2);
        LinkedList<Double> probability = new LinkedList<Double>(Arrays.asList(1.0)); // must be 1

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(nextTarget);
        attributesTargetChanger.setProbabilitiesToChangeTarget(probability);

        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        assertEquals(nextTargetIndex, pedestrians.get(1).getNextTargetListIndex());

        controllerUnderTest.update(simTimeInSec);

        assertEquals(0, pedestrians.get(1).getNextTargetListIndex());
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), nextTarget);
    }

    @Test
    public void updateDoesNotChangeTargetListOfAffectedPedestrianIfProbabilityIsZero() {
        LinkedList<Integer> nextTarget = createIntegerList(2);
        LinkedList<Double> probability = new LinkedList<Double>(Arrays.asList(0.0)); // must be 0

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(nextTarget);
        attributesTargetChanger.setProbabilitiesToChangeTarget(probability);

        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        int expectedTargetId = 1;
        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), expectedTargetId);
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), expectedTargetId);

        controllerUnderTest.update(simTimeInSec);

        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), expectedTargetId);
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), expectedTargetId);
    }

    @Test
    public void targetChangerWithListOfTargetsAndStaticTargets() {
        LinkedList<Integer> nextTarget = createIntegerList(2, 1);
        LinkedList<Double> probability = new LinkedList<Double>(Arrays.asList(1.0,1.0)); // must be 1
        pedestrians.forEach(p -> p.setTargets(createIntegerList(1)));

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(nextTarget);
        attributesTargetChanger.setProbabilitiesToChangeTarget(probability);

        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), createIntegerList(1));
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), createIntegerList(1));

        controllerUnderTest.update(simTimeInSec);

        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), createIntegerList(1));
        assertListEqual(pedestrians.get(1).getTargets(), createIntegerList(2, 1));
    }

    @Test
    public void targetChangerSkipFirstTargetInListOftaticTargets() {
        LinkedList<Integer> nextTarget = createIntegerList(2, 1);
        LinkedList<Double> probability = new LinkedList<Double>(Arrays.asList(0.0,1.0)); // must be 1
        pedestrians.forEach(p -> p.setTargets(createIntegerList(1)));

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(nextTarget);
        attributesTargetChanger.setProbabilitiesToChangeTarget(probability);

        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), createIntegerList(1));
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), createIntegerList(1));

        controllerUnderTest.update(simTimeInSec);

        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), createIntegerList(1));
        assertListEqual(pedestrians.get(1).getTargets(), createIntegerList( 1));
    }



    @Test
    public void targetChangerWithListOfTargetsAndDynamicTargets() { //must choose first element
        LinkedList<Integer> nextTarget = createIntegerList(1, 2);
        LinkedList<Double> probability = new LinkedList<Double>(Arrays.asList(1.0)); // must be 1
        pedestrians.forEach(p -> p.setTargets(createIntegerList(1)));

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(nextTarget);
        attributesTargetChanger.setProbabilitiesToChangeTarget(probability);
        attributesTargetChanger.setNextTargetIsPedestrian(true);

        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), createIntegerList(1));
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), createIntegerList(1));

        controllerUnderTest.update(simTimeInSec);

        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), createIntegerList(1));
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), 1 + TargetPedestrian.UNIQUE_ID_OFFSET);
    }

    @Test
    public void updateAddsTargetPedestrianToTopographyIfTargetIsDynamic() {
        LinkedList<Integer> nextTarget = createIntegerList(1);
        LinkedList<Double> probability = new LinkedList<Double>(Arrays.asList(1.0)); // must be 1

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(nextTarget);
        attributesTargetChanger.setNextTargetIsPedestrian(true);
        attributesTargetChanger.setProbabilitiesToChangeTarget(probability);

        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        List<Target> targetPedestrians = topography.getTargets().stream().filter(target -> target instanceof TargetPedestrian).collect(Collectors.toList());
        assertEquals(0, targetPedestrians.size());

        controllerUnderTest.update(simTimeInSec);

        targetPedestrians = topography.getTargets().stream().filter(target -> target instanceof TargetPedestrian).collect(Collectors.toList());
        assertEquals(1, targetPedestrians.size());
    }

    @Test
    public void updateChangesTargetListOfAffectedPedestrianIfTargetIsDynamic() {
        LinkedList<Integer> nextTarget = createIntegerList(1);
        LinkedList<Double> probability = new LinkedList<Double>(Arrays.asList(1.0)); // must be 1

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(nextTarget);
        attributesTargetChanger.setNextTargetIsPedestrian(true);
        attributesTargetChanger.setProbabilitiesToChangeTarget(probability);

        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        int expectedTargetId = 1;
        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), expectedTargetId);
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), expectedTargetId);

        controllerUnderTest.update(simTimeInSec);

        int expectedTargetIdForPed2 = pedestrians.get(0).getId() + TargetPedestrian.UNIQUE_ID_OFFSET;
        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), expectedTargetId);
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), expectedTargetIdForPed2);
    }

    @Test
    public void updateModifiesFollowersIfTargetIsDynamic() {
        LinkedList<Integer> nextTarget = createIntegerList(1);
        LinkedList<Double> probability = new LinkedList<Double>(Arrays.asList(1.0)); // must be 1

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(nextTarget);
        attributesTargetChanger.setNextTargetIsPedestrian(true);
        attributesTargetChanger.setProbabilitiesToChangeTarget(probability);

        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        assertTrue(pedestrians.get(0).getFollowers().isEmpty());

        controllerUnderTest.update(simTimeInSec);

        LinkedList<Agent> followers = pedestrians.get(0).getFollowers();
        assertEquals(1, followers.size());
        assertEquals(pedestrians.get(1).getId(), followers.get(0).getId());
    }

    @Test
    public void updateModifiesPedestrianWithExistingFollwersIfTargetIsDynamic() {
        LinkedList<Integer> nextTarget = createIntegerList(1);
        LinkedList<Double> probability = new LinkedList<Double>(Arrays.asList(1.0)); // must be 1


        // Add two new agents were one follows the other.
        List<Pedestrian> newPedestrians = createTwoPedestrianWithTargetT1(3);
        LinkedList<Agent> follower = new LinkedList<>();
        follower.add(newPedestrians.get(1));

        newPedestrians.get(0).setPosition(new VPoint(1, 2));
        newPedestrians.get(1).setPosition(new VPoint(1, 3));
        newPedestrians.get(0).setFollowers(follower);

        for (Pedestrian pedestrian : newPedestrians) {
            topography.addElement(pedestrian);
        }

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(nextTarget);
        attributesTargetChanger.setNextTargetIsPedestrian(true);
        attributesTargetChanger.setProbabilitiesToChangeTarget(probability);

        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        assertEquals(0, pedestrians.get(0).getFollowers().size());
        assertEquals(1, newPedestrians.get(0).getFollowers().size());

        controllerUnderTest.update(simTimeInSec);

        assertEquals(0, pedestrians.get(0).getFollowers().size());
        assertEquals(2, newPedestrians.get(0).getFollowers().size());
    }

    @Test
    public void updateUseStaticTargetAsFallbackIfNoPedestrianIsFoundIfTargetIsDynamic() {
        LinkedList<Integer> nextTarget = createIntegerList(3);
        LinkedList<Double> probability = new LinkedList<Double>(Arrays.asList(1.0)); // must be 1

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(nextTarget);
        attributesTargetChanger.setNextTargetIsPedestrian(true);
        attributesTargetChanger.setProbabilitiesToChangeTarget(probability);

        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        int expectedTargetId = 1;
        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), expectedTargetId);
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), expectedTargetId);

        controllerUnderTest.update(simTimeInSec);

        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), 1);
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), nextTarget);

        List<Target> targetPedestrians = topography.getTargets().stream().filter(target -> target instanceof TargetPedestrian).collect(Collectors.toList());
        assertEquals(0, targetPedestrians.size());
    }

    private void assertListContainsSingleTarget(LinkedList<Integer> targetList, Integer targetId) {
        assertEquals(1, targetList.size());
        assertEquals(targetList.getFirst(), targetId);
    }

    private void assertListContainsSingleTarget(LinkedList<Integer> targetList, LinkedList<Integer> targetId) {
        assertEquals(1, targetList.size());
        assertEquals(targetList.getFirst(), targetId.getFirst());
    }

    private void assertListEqual(LinkedList<Integer> targetList, LinkedList<Integer> targetId) {
        assertEquals(targetList, targetId);
    }


}