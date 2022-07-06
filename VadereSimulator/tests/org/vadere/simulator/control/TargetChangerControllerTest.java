package org.vadere.simulator.control;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.control.scenarioelements.TargetChangerController;
import org.vadere.simulator.control.scenarioelements.targetchanger.TargetChangerAlgorithm;
import org.vadere.simulator.models.groups.cgm.CentroidGroup;
import org.vadere.simulator.models.groups.cgm.CentroidGroupModel;
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

    private LinkedList<Integer> createIntegerList(Integer... integers) {
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

        Pedestrian pedestrian2 = new Pedestrian(new AttributesAgent(startId + 1), random);
        pedestrian2.setPosition(new VPoint(5, 2));
        pedestrian2.setTargets(targetsPed2);

        // Watch out: Use an "ArrayList" to keep order and
        // index 0 refers to pedestrian p1!
        List<Pedestrian> pedestrians = new ArrayList<>();
        pedestrians.add(pedestrian1);
        pedestrians.add(pedestrian2);

        return pedestrians;
    }

    private List<Pedestrian> createGroupOfPedestriansTargetT1(int startId) {
        int seed = 0;
        Random random = new Random(seed);
        CentroidGroup cg = new CentroidGroup(1, 2, new CentroidGroupModel());


        LinkedList<Integer> target = new LinkedList<>();
        target.add(1);

        LinkedList<Integer> groupId = new LinkedList<>();
        groupId.add(42);

        Pedestrian pedestrian1 = new Pedestrian(new AttributesAgent(startId), random);
        pedestrian1.setPosition(new VPoint(5, 1));
        pedestrian1.setTargets(target);
        cg.addMember(pedestrian1);
        pedestrian1.addAgentListener(cg);
        pedestrian1.setGroupIds(groupId);

        Pedestrian pedestrian2 = new Pedestrian(new AttributesAgent(startId + 1), random);
        pedestrian2.setPosition(new VPoint(1, 1));
        pedestrian2.setTargets(target);
        cg.addMember(pedestrian2);
        pedestrian2.addAgentListener(cg);
        pedestrian2.setGroupIds(groupId);

        LinkedList<Pedestrian> list1 = new LinkedList<>();
        list1.add(pedestrian1);

        LinkedList<Pedestrian> list2 = new LinkedList<>();
        list2.add(pedestrian2);

        pedestrian1.setAgentsInGroup(list2);
        pedestrian2.setAgentsInGroup(list1);

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
        AttributesTargetChanger a = new AttributesTargetChanger(new VRectangle(4, 1, 2, 8), 1);
        a.setNextTarget(createIntegerList(-1));
        return a;
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

        List<Pedestrian> processedAgents = new LinkedList<>();
        pedestrians.forEach(pedestrian -> {
            if (pedestrian.getElementsEncountered(TargetChanger.class).contains(targetChanger.getId())) {
                processedAgents.add(pedestrian);
            }
        });
        assertTrue(processedAgents.isEmpty());
    }

    @Test
    public void updateProcessesOnlyAgentsWithinTargetChanger() {
        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);

        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        controllerUnderTest.update(simTimeInSec);

        Map<Integer, Agent> processedAgents = new HashMap<>();
        pedestrians.forEach(pedestrian -> {
            if (pedestrian.getElementsEncountered(TargetChanger.class).contains(targetChanger.getId())) {
                processedAgents.put(pedestrian.getId(), pedestrian);
            }
        });

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
        LinkedList<Double> probability = new LinkedList<Double>(Arrays.asList(1.0, 1.0)); // must be 1
        pedestrians.forEach(p -> p.setTargets(createIntegerList(1)));

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(nextTarget);
        attributesTargetChanger.setProbabilitiesToChangeTarget(probability);
        attributesTargetChanger.setChangeAlgorithmType(TargetChangerAlgorithmType.SORTED_SUB_LIST);

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
        LinkedList<Double> probability = new LinkedList<Double>(Arrays.asList(0.0, 1.0)); // must be 1
        pedestrians.forEach(p -> p.setTargets(createIntegerList(1)));

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(nextTarget);
        attributesTargetChanger.setChangeAlgorithmType(TargetChangerAlgorithmType.SORTED_SUB_LIST);
        attributesTargetChanger.setProbabilitiesToChangeTarget(probability);

        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), createIntegerList(1));
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), createIntegerList(1));

        controllerUnderTest.update(simTimeInSec);

        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), createIntegerList(1));
        assertListEqual(pedestrians.get(1).getTargets(), createIntegerList(1));
    }


    @Test
    public void targetChangerWithListOfTargetsAndDynamicTargets() { //must choose first element
        LinkedList<Integer> nextTarget = createIntegerList(1);
        LinkedList<Double> probability = new LinkedList<Double>(Arrays.asList(1.0)); // must be 1
        pedestrians.forEach(p -> p.setTargets(createIntegerList(1)));

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(nextTarget);
        attributesTargetChanger.setProbabilitiesToChangeTarget(probability);
        attributesTargetChanger.setChangeAlgorithmType(TargetChangerAlgorithmType.FOLLOW_PERSON);

        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), createIntegerList(1));
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), createIntegerList(1));

        controllerUnderTest.update(simTimeInSec);

        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), createIntegerList(1));
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), 1 + TargetPedestrian.UNIQUE_ID_OFFSET);
    }

    @Test
    public void targetChangerSelectOneOffList() { //must choose first element
        LinkedList<Integer> nextTarget = createIntegerList(1, 2, 3);
        LinkedList<Double> probability = new LinkedList<Double>(Arrays.asList(0.0, 0.0, 1.0)); // must be 1
        pedestrians.forEach(p -> p.setTargets(createIntegerList(1)));

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(nextTarget);
        attributesTargetChanger.setProbabilitiesToChangeTarget(probability);
        attributesTargetChanger.setChangeAlgorithmType(TargetChangerAlgorithmType.SELECT_ELEMENT);

        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), createIntegerList(1));
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), createIntegerList(1));

        controllerUnderTest.update(simTimeInSec);

        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), createIntegerList(1));
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), 3);
    }

    @Test
    public void targetChangerSelectOneOffListOrDoNothing() { //must choose first element
        LinkedList<Integer> nextTarget = createIntegerList(1, 2, 3);
        // probability size == nextTarget.size + 1. Last probability means change nothing.
        LinkedList<Double> probability = new LinkedList<Double>(Arrays.asList(0.0, 0.0, 0.0, 1.0)); // must be 1
        pedestrians.forEach(p -> p.setTargets(createIntegerList(1)));

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(nextTarget);
        attributesTargetChanger.setProbabilitiesToChangeTarget(probability);
        attributesTargetChanger.setChangeAlgorithmType(TargetChangerAlgorithmType.SELECT_ELEMENT);

        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), createIntegerList(1));
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), createIntegerList(1));

        controllerUnderTest.update(simTimeInSec);

        assertListContainsSingleTarget(pedestrians.get(0).getTargets(), createIntegerList(1));
        assertListContainsSingleTarget(pedestrians.get(1).getTargets(), createIntegerList(1));
    }

    @Test
    public void targetChangerSelectOneOffListRelativeProb() { //must choose first element
        LinkedList<Integer> nextTarget = createIntegerList(2, 3, 4);
        LinkedList<Double> probability = new LinkedList<Double>(Arrays.asList(10.0, 10.0, 20.0)); // (.25, .25, .50)
        pedestrians.forEach(p -> p.setTargets(createIntegerList(1)));

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(nextTarget);
        attributesTargetChanger.setProbabilitiesToChangeTarget(probability);
        attributesTargetChanger.setChangeAlgorithmType(TargetChangerAlgorithmType.SELECT_ELEMENT);

        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        Pedestrian p = pedestrians.get(1);
        ArrayList<Integer> targets = new ArrayList<>();
        TargetChangerAlgorithm alg = controllerUnderTest.getChangerAlgorithm();
        for (int i = 0; i < 10000; i++) {
            p.setTargets(createIntegerList(1));
            alg.setAgentTargetList(p);
            assertEquals(1, p.getTargets().size());
            targets.add(p.getTargets().getFirst());
        }
        Map<Integer, Long> counts = targets.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        assertEquals(0.25, (double) counts.get(2) / 10000, 0.01);
        assertEquals(0.25, (double) counts.get(3) / 10000, 0.01);
        assertEquals(0.5, (double) counts.get(4) / 10000, 0.01);
    }

    @Test
    public void targetChangerSelectOneOffListSameRelativeProb() { //must choose first element
        LinkedList<Integer> nextTarget = createIntegerList(2, 3, 4);
        LinkedList<Double> probability = new LinkedList<Double>(); // (.33, .33, .33)
        pedestrians.forEach(p -> p.setTargets(createIntegerList(1)));

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(nextTarget);
        attributesTargetChanger.setProbabilitiesToChangeTarget(probability);
        attributesTargetChanger.setChangeAlgorithmType(TargetChangerAlgorithmType.SELECT_ELEMENT);

        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);

        Pedestrian p = pedestrians.get(1);
        ArrayList<Integer> targets = new ArrayList<>();
        TargetChangerAlgorithm alg = controllerUnderTest.getChangerAlgorithm();
        for (int i = 0; i < 10000; i++) {
            p.setTargets(createIntegerList(1));
            alg.setAgentTargetList(p);
            assertEquals(1, p.getTargets().size());
            targets.add(p.getTargets().getFirst());
        }
        Map<Integer, Long> counts = targets.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        assertEquals(0.33, (double) counts.get(2) / 10000, 0.01);
        assertEquals(0.33, (double) counts.get(3) / 10000, 0.01);
        assertEquals(0.33, (double) counts.get(4) / 10000, 0.01);
    }

    @Test
    public void updateAddsTargetPedestrianToTopographyIfTargetIsDynamic() {
        LinkedList<Integer> nextTarget = createIntegerList(1);
        LinkedList<Double> probability = new LinkedList<Double>(Arrays.asList(1.0)); // must be 1

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(nextTarget);
        attributesTargetChanger.setChangeAlgorithmType(TargetChangerAlgorithmType.FOLLOW_PERSON);
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
        attributesTargetChanger.setChangeAlgorithmType(TargetChangerAlgorithmType.FOLLOW_PERSON);
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
        attributesTargetChanger.setChangeAlgorithmType(TargetChangerAlgorithmType.FOLLOW_PERSON);
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
        attributesTargetChanger.setChangeAlgorithmType(TargetChangerAlgorithmType.FOLLOW_PERSON);
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
        attributesTargetChanger.setChangeAlgorithmType(TargetChangerAlgorithmType.FOLLOW_PERSON);
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

    // check IllegalArgumentException in nextTarget and probability size setup for each algorithm

    @Test(expected = IllegalArgumentException.class)
    public void checkFollowPedestrianTarget() {
        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(createIntegerList(1, 2, 3));
        attributesTargetChanger.setChangeAlgorithmType(TargetChangerAlgorithmType.FOLLOW_PERSON);
        attributesTargetChanger.setProbabilitiesToChangeTarget(new LinkedList<Double>(Arrays.asList(1.0)));

        // only one nextTarget needed
        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkFollowPedestrianTargetWronProb() {
        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(createIntegerList(1, 2, 3));
        attributesTargetChanger.setChangeAlgorithmType(TargetChangerAlgorithmType.FOLLOW_PERSON);
        attributesTargetChanger.setProbabilitiesToChangeTarget(new LinkedList<Double>(Arrays.asList(1.1)));

        // only one nextTarget needed
        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkSelectElementTargetToFew() {
        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(createIntegerList(1, 2, 3, 4));
        attributesTargetChanger.setChangeAlgorithmType(TargetChangerAlgorithmType.SELECT_ELEMENT);
        attributesTargetChanger.setProbabilitiesToChangeTarget(new LinkedList<Double>(
                Arrays.asList(0.7, 0.5)));

        // to few probabilities. (same size or one more than nextTargets)
        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkSelectElementTargetToMany() {
        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(createIntegerList(1, 2, 3, 4));
        attributesTargetChanger.setChangeAlgorithmType(TargetChangerAlgorithmType.SELECT_ELEMENT);
        attributesTargetChanger.setProbabilitiesToChangeTarget(new LinkedList<Double>(
                Arrays.asList(0.7, 0.5, 0.1, 0.1, 0.0, 0.0)));

        // to many probabilities. (same size or one more than nextTargets)
        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkListTarget() {
        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(createIntegerList(1, 2, 3, 4));
        attributesTargetChanger.setChangeAlgorithmType(TargetChangerAlgorithmType.SELECT_LIST);
        attributesTargetChanger.setProbabilitiesToChangeTarget(new LinkedList<Double>(
                Arrays.asList(0.7, 0.5)));

        // only one probability.

        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkSubListTarget() {
        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setNextTarget(createIntegerList(1, 2, 3, 4));
        attributesTargetChanger.setChangeAlgorithmType(TargetChangerAlgorithmType.SORTED_SUB_LIST);
        attributesTargetChanger.setProbabilitiesToChangeTarget(new LinkedList<Double>(
                Arrays.asList(0.7, 1.0, 0.3)));

        // must be same number
        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = createTargetChangerController(targetChanger);


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

    @Test
    public void updateChangesTargetGroup() {

        Topography topography = new Topography();
        List<Pedestrian> peds = createGroupOfPedestriansTargetT1(1);
        List<Target> targs = createTwoTargets();

        for (Pedestrian pedestrian : peds) {
            topography.addElement(pedestrian);
        }
        for (Target target : targs) {
            topography.addTarget(target);
        }

        LinkedList<Double> probability = new LinkedList<Double>(Arrays.asList(1.0));

        AttributesTargetChanger attributesTargetChanger = createAttributesWithFixedRectangle();
        attributesTargetChanger.setChangeAlgorithmType(TargetChangerAlgorithmType.SELECT_LIST);
        attributesTargetChanger.setNextTarget(createIntegerList(2));
        attributesTargetChanger.setProbabilitiesToChangeTarget(probability);

        TargetChanger targetChanger = new TargetChanger(attributesTargetChanger);
        TargetChangerController controllerUnderTest = new TargetChangerController(topography, targetChanger, new Random(0));


        assertEquals(1, peds.get(0).getNextTargetId());
        assertEquals(1, peds.get(1).getNextTargetId());

        controllerUnderTest.update(simTimeInSec);

        assertEquals(2, peds.get(0).getNextTargetId());
        assertEquals(2, peds.get(1).getNextTargetId());

    }


}