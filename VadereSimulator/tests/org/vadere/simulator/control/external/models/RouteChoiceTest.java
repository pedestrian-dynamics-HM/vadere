package org.vadere.simulator.control.external.models;


import org.apache.commons.math3.util.Precision;
import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.ElapsedTime;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class RouteChoiceTest {


    private List<Pedestrian> createPedestrians(int totalPedestrians) {
        LinkedList<Integer> initialTarget = new LinkedList<>();
        initialTarget.add(5);

        List<Pedestrian> pedestrians = new ArrayList<>();

        for (int i = 0; i < totalPedestrians; i++) {
            long seed = 0;
            Random random = new Random(seed);
            AttributesAgent attributesAgent = new AttributesAgent(i);

            Pedestrian currentPedestrian = new Pedestrian(attributesAgent, random);

            currentPedestrian.setMostImportantStimulus(new ElapsedTime());
            currentPedestrian.setSelfCategory(SelfCategory.TARGET_ORIENTED);

            currentPedestrian.setTargets(initialTarget);

            pedestrians.add(currentPedestrian);
        }

        return pedestrians;
    }

    private Topography createTopography(List<Pedestrian> initialPedestrians) {
        Topography topography = new Topography();

        initialPedestrians.stream().forEach(pedestrian -> topography.addElement(pedestrian));

        List<Target> targets = createTwoTargets();
        targets.stream().forEach(target -> topography.addTarget(target));

        return topography;
    }

    private ArrayList<Target> createTwoTargets() {
        ArrayList<Target> targets = new ArrayList<>();

        Target target1 = createTarget(new VPoint(0, 0), 1, 0);
        Target target2 = createTarget(new VPoint(5, 0), 1, 1);
        Target target3 = createTarget(new VPoint(10, 0), 1, 1);


        targets.add(target1);
        targets.add(target2);
        targets.add(target3);

        return targets;
    }

    private Target createTarget(VPoint center, double radius, int id) {
        VShape shape = new VCircle(center, radius);
        boolean absorbing = true;

        AttributesTarget attributesTarget = new AttributesTarget(shape, id, absorbing);
        Target target = new Target(attributesTarget);

        return target;
    }

    private String readInputFile(){
        String dataPath = "testResources/control/external/CorridorChoiceData.json";
        String msg = "";

        try {
            msg = IOUtils.readTextFile(dataPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msg;
    }

    private String readInputFile3(){
        String dataPath = "testResources/control/external/CorridorChoiceData3.json";
        String msg = "";

        try {
            msg = IOUtils.readTextFile(dataPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msg;
    }




    private String readInputFile2(){
        String dataPath = "testResources/control/external/CorridorChoiceData2.json";
        String msg = "";

        try {
            msg = IOUtils.readTextFile(dataPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msg;
    }


    @Test
    public void updateState() {

        int nr_peds = 10000;
        double[] probs = { 0.25, 0, 0.25, 0.5 };
        int[] targets = { 1, 2, 3, 4};
        String msg = readInputFile();

        RouteChoice routeChoice = new RouteChoice();
        Topography topo = createTopography(createPedestrians(nr_peds));

        routeChoice.update(topo, manager.getRemoteSimulationRun().getStimulusController(), -1.0, msg, specify_id);

        double prob;
        for (int i = 0; i < targets.length; i++) {
            int t = targets[i];
            prob = 1.0 * topo.getPedestrianDynamicElements().getElements().stream().filter(p -> p.getTargets().get(0) == t).count() / nr_peds;
            assertTrue( Precision.equals(prob, probs[i], 0.02));
        }

    }

    @Test
    public void timeIn() {

        // Create a single pedestrian with initial target nr. 5
        Topography topo = createTopography(createPedestrians(1));

        String msg = readInputFile();
        RouteChoice routeChoice = new RouteChoice();
        routeChoice.update(topo, manager.getRemoteSimulationRun().getStimulusController(), 6.0, msg, specify_id);

        Stimulus target = topo.getPedestrianDynamicElements().getElement(0).getMostImportantStimulus();
        //TODO what to check here? assertEquals("ChangeTarget", target.toString());

    }

    @Test
    public void timeOut() {

        // Create a single pedestrian with initial target nr. 5
        Topography topo = createTopography(createPedestrians(1));

        String msg = readInputFile();
        RouteChoice routeChoice = new RouteChoice();
        // keep old target nr.5 because timeout reached
        routeChoice.update(topo, manager.getRemoteSimulationRun().getStimulusController(), 10.0, msg, specify_id);

        int target = topo.getPedestrianDynamicElements().getElement(0).getNextTargetId();
        assertEquals(target, 5);

    }

    @Test
    public void pedOutOfArea() {

        // Create a single pedestrian with initial target nr. 5
        Topography topo = createTopography(createPedestrians(1));
        Pedestrian ped =  topo.getPedestrianDynamicElements().getElement(0);
        ped.setPosition(new VPoint(-1000.,1000.));

        String msg = readInputFile();
        RouteChoice routeChoice = new RouteChoice();
        // keep old target nr.5 because timeout reached
        routeChoice.update(topo, manager.getRemoteSimulationRun().getStimulusController(), -1., msg, specify_id);

        int target = ped.getNextTargetId();
        assertEquals(target, 5);
    }

    @Test
    public void pedInArea() {

        // Create a single pedestrian with initial target nr. 5
        Topography topo = createTopography(createPedestrians(1));
        Pedestrian ped =  topo.getPedestrianDynamicElements().getElement(0);
        ped.setPosition(new VPoint(0.,0.));

        String msg = readInputFile();
        RouteChoice routeChoice = new RouteChoice();
        // keep old target nr.5 because timeout reached
        routeChoice.update(topo, manager.getRemoteSimulationRun().getStimulusController(), -1., msg, specify_id);

        int target = ped.getNextTargetId();
        assertEquals(target, 4);
    }

    @Test
    public void pedInRectangle() {

        // Create a single pedestrian with initial target nr. 5
        Topography topo = createTopography(createPedestrians(1));
        Pedestrian ped =  topo.getPedestrianDynamicElements().getElement(0);
        ped.setPosition(new VPoint(5.,5.));

        String msg = readInputFile3();
        RouteChoice routeChoice = new RouteChoice();
        // keep old target nr.5 because timeout reached
        routeChoice.update(topo, manager.getRemoteSimulationRun().getStimulusController(), -1., msg, specify_id);

        int target = ped.getNextTargetId();
        assertEquals(target, 2);
    }




    public void commandIdMissing() {
        // the route choice app requires a unique command id.

        Topography topo = createTopography(createPedestrians(1));
        Pedestrian ped =  topo.getPedestrianDynamicElements().getElement(0);
        ped.setPosition(new VPoint(0.,0.));

        String msg = readInputFile2();
        RouteChoice routeChoice = new RouteChoice();
        routeChoice.update(topo, manager.getRemoteSimulationRun().getStimulusController(), -1., msg, specify_id);
    }

    @Test
    public void testCommandIdMissingWrapper() {
        try {
            commandIdMissing();
        } catch (final IllegalArgumentException e) {
            assertTrue(e.getMessage().equals("Please provide a unique commandId != 0 for each command. Otherwise, information might not be processed."));
        }
    }

    @Test
    public void testHandleRecurringInformation() {

        // Create a single pedestrian with initial target nr. 5
        Topography topo = createTopography(createPedestrians(1));
        Pedestrian ped =  topo.getPedestrianDynamicElements().getElement(0);
        ped.setPosition(new VPoint(0.,0.));

        RouteChoice routeChoice = new RouteChoice();
        // keep old target nr.5 because timeout reached
        routeChoice.update(topo, manager.getRemoteSimulationRun().getStimulusController(), -1., readInputFile(), specify_id);
        assertEquals(ped.getNextTargetId(), 4);

        // readInputFile3() provides target 2. it is skipped because the command id is the same.
        routeChoice.update(topo, manager.getRemoteSimulationRun().getStimulusController(), -1., readInputFile3(), specify_id);
        assertEquals(ped.getNextTargetId(), 4);

    }




}
