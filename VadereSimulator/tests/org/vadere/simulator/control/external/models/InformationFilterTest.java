package org.vadere.simulator.control.external.models;

import org.junit.Test;
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
import org.vadere.util.geometry.shapes.VShape;

import java.util.*;

import static org.junit.Assert.assertFalse;

public class InformationFilterTest {


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

    @Test
    public void testCommamdIdFreeAnsApplicable(){
        int commandId = 55;
        Topography topo = createTopography(createPedestrians(2));
        InformationFilter filter = new InformationFilter(true, false);

        for (Pedestrian ped : topo.getPedestrianDynamicElements().getElements()){
            assert(filter.isProcessFirstInformationOnly(ped));
        }
    }

    @Test
    public void testSecondCommandId(){
        Topography topo = createTopography(createPedestrians(2));
        InformationFilter filter = new InformationFilter(false, false);

        for (Pedestrian ped : topo.getPedestrianDynamicElements().getElements()){
            filter.setPedProcessedCommandIds(ped, 33);
            assert(filter.isProcessFirstInformationOnly(ped));
        }
    }

    @Test
    public void testCommamdAlreadyUsedAndApplicable(){
        int commandId = 55;
        Topography topo = createTopography(createPedestrians(2));
        InformationFilter filter = new InformationFilter(false, true);

        for (Pedestrian ped : topo.getPedestrianDynamicElements().getElements()){
            filter.setPedProcessedCommandIds(ped, commandId);
            assert(filter.isProcessSameInfoAgain(ped, commandId));
            assert(filter.isProcessFirstInformationOnly(ped));
        }
    }

    @Test
    public void testCommamdAlreadyUsedAndNotApplicable(){
        int commandId = 55;
        Topography topo = createTopography(createPedestrians(2));
        InformationFilter filter = new InformationFilter(false, false);

        for (Pedestrian ped : topo.getPedestrianDynamicElements().getElements()){
            filter.setPedProcessedCommandIds(ped, commandId);
            assertFalse(filter.isProcessSameInfoAgain(ped, commandId));
            assert(filter.isProcessFirstInformationOnly(ped));
        }
    }


    @Test
    public void testPedInNullArea() {
        int commandId = 55;
        Topography topo = createTopography(createPedestrians(2));
        InformationFilter filter = new InformationFilter(false, false);

        VRectangle rectangle = new VRectangle(0, 0, 5, 5);
        Collection<Pedestrian> peds = topo.getPedestrianDynamicElements().getElements();
        peds.stream().forEach(pedestrian -> pedestrian.setPosition(new VPoint(1, 4.0 * pedestrian.getId())));


        for (Pedestrian ped : topo.getPedestrianDynamicElements().getElements()) {
            filter.setPedProcessedCommandIds(ped, commandId);
            assert (filter.isPedInDefinedArea(ped, null));
        }
    }

        @Test
        public void testPedInArea() {
            int commandId = 55;
            Topography topo = createTopography(createPedestrians(2));
            InformationFilter filter = new InformationFilter(false, false);

            VRectangle rectangle = new VRectangle(0, 0, 5, 5);
            Collection<Pedestrian> peds = topo.getPedestrianDynamicElements().getElements();
            peds.stream().forEach(pedestrian -> pedestrian.setPosition(new VPoint(1, 4.0 * pedestrian.getId())));


            for (Pedestrian ped : topo.getPedestrianDynamicElements().getElements()) {
                filter.setPedProcessedCommandIds(ped, commandId);
                if (ped.getId() == 2) {
                    // only ped with id = 2 is outside area
                    assertFalse(filter.isPedInDefinedArea(ped, rectangle));
                } else assert (filter.isPedInDefinedArea(ped, rectangle));
            }
        }









}
