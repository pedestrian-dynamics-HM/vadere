package org.vadere.simulator.control.psychology.cognition;

import org.junit.Test;
import org.vadere.simulator.control.psychology.cognition.models.CounterflowCognitionModel;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.ElapsedTime;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class CounterflowCognitionModelTest {

    private List<Attributes> attributes = new LinkedList<>();


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

        targets.add(target1);
        targets.add(target2);

        return targets;
    }

    private Target createTarget(VPoint center, double radius, int id) {
        VShape shape = new VCircle(center, radius);
        boolean absorbing = true;

        AttributesTarget attributesTarget = new AttributesTarget(shape, id, absorbing);
        Target target = new Target(attributesTarget);

        return target;
    }

    private void movePedestrian(Pedestrian pedestrian, VPoint newPosition, Topography topography) {
        VPoint oldPosition = pedestrian.getPosition();
        pedestrian.setPosition(newPosition);
        topography.moveElement(pedestrian, oldPosition);
    }

    @Test
    public void updateDoesNotChangeSelfCategoryIfPedHasNoTarget() {
        boolean usePedIdAsTargetId = false;
        List<Pedestrian> pedestrians = createPedestrians(2, usePedIdAsTargetId);
        Topography topography = createTopography(pedestrians);

        CounterflowCognitionModel counterflowCognitionModel = new CounterflowCognitionModel();
        counterflowCognitionModel.initialize(topography, new Random(0));

        counterflowCognitionModel.update(pedestrians);

        SelfCategory expectedSelfCategory = SelfCategory.TARGET_ORIENTED;
        for (Pedestrian pedestrian : pedestrians) {
            assertEquals(expectedSelfCategory, pedestrian.getSelfCategory());
        }
    }

    @Test
    public void updateDoesNotChangeSelfCategoryIfNoNeighborIsAvailable() {
        boolean usePedIdAsTargetId = true;
        List<Pedestrian> pedestrians = createPedestrians(2, usePedIdAsTargetId);
        Topography topography = createTopography(pedestrians);

        // Search radius is expected to be 1 m.
        movePedestrian(pedestrians.get(0), new VPoint(0,0), topography);
        movePedestrian(pedestrians.get(1), new VPoint(2,0), topography);

        CounterflowCognitionModel counterflowCognitionModel = new CounterflowCognitionModel();
        counterflowCognitionModel.initialize(topography, new Random(0));

        counterflowCognitionModel.update(pedestrians);

        SelfCategory expectedSelfCategory = SelfCategory.TARGET_ORIENTED;
        for (Pedestrian pedestrian : pedestrians) {
            assertEquals(expectedSelfCategory, pedestrian.getSelfCategory());
        }
    }


    @Test
    public void updateDoesNotChangeSelfCategoryIfNeighborIsFurtherAwayFromOwnTarget() {
        boolean usePedIdAsTargetId = true;
        List<Pedestrian> pedestrians = createPedestrians(2, usePedIdAsTargetId);
        Topography topography = createTopography(pedestrians);

        movePedestrian(pedestrians.get(0), new VPoint(0,0), topography);
        movePedestrian(pedestrians.get(1), new VPoint(0.5,0), topography);

        CounterflowCognitionModel counterflowCognitionModel = new CounterflowCognitionModel();
        counterflowCognitionModel.initialize(topography, new Random(0));

        counterflowCognitionModel.update(pedestrians);

        SelfCategory expectedSelfCategory = SelfCategory.TARGET_ORIENTED;
        for (Pedestrian pedestrian : pedestrians) {
            assertEquals(expectedSelfCategory, pedestrian.getSelfCategory());
        }
    }

    @Test
    public void updateChangesSelfCategoryIfNeighborIsCloserToTargetAndWalkingDirectionDiffers() {
        boolean usePedIdAsTargetId = true;
        List<Pedestrian> pedestrians = createPedestrians(2, usePedIdAsTargetId);
        Topography topography = createTopography(pedestrians);

        movePedestrian(pedestrians.get(0), new VPoint(2.5,0), topography);
        movePedestrian(pedestrians.get(1), new VPoint(2,0), topography);

        CounterflowCognitionModel counterflowCognitionModel = new CounterflowCognitionModel();
        counterflowCognitionModel.initialize(topography, new Random(0));

        counterflowCognitionModel.update(pedestrians);

        SelfCategory expectedSelfCategory = SelfCategory.EVADE;
        for (Pedestrian pedestrian : pedestrians) {
            assertEquals(expectedSelfCategory, pedestrian.getSelfCategory());
        }
    }

    @Test
    public void updateDoesNotChangeSelfCategoryIfNeighborIsCloserToTargetButWalkingDirectionIsSame() {
        boolean usePedIdAsTargetId = true;
        List<Pedestrian> pedestrians = createPedestrians(2, usePedIdAsTargetId);
        Topography topography = createTopography(pedestrians);

        pedestrians.get(1).setTargets(pedestrians.get(0).getTargets());
        movePedestrian(pedestrians.get(0), new VPoint(2.5,0), topography);
        movePedestrian(pedestrians.get(1), new VPoint(2,0), topography);

        CounterflowCognitionModel counterflowCognitionModel = new CounterflowCognitionModel();
        counterflowCognitionModel.initialize(topography, new Random(0));

        counterflowCognitionModel.update(pedestrians);

        SelfCategory expectedSelfCategory = SelfCategory.TARGET_ORIENTED;
        for (Pedestrian pedestrian : pedestrians) {
            assertEquals(expectedSelfCategory, pedestrian.getSelfCategory());
        }
    }

}