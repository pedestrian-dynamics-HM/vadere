package org.vadere.simulator.control.psychology.perception;

import org.junit.Test;
import org.vadere.simulator.control.psychology.perception.models.SimplePerceptionModel;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.psychology.perception.AttributesSimplePerceptionModel;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import java.util.*;

import static org.junit.Assert.*;

public class SimplePerceptionModelTest {

    private static double ALLOWED_DOUBLE_ERROR = 10e-3;
    private List<Attributes> attributesList = Collections.singletonList(new AttributesSimplePerceptionModel());


    private List<Pedestrian> createPedestrians(int totalPedestrians) {
        List<Pedestrian> pedestrians = new ArrayList<>();

        for (int i = 0; i < totalPedestrians; i++) {
            long seed = 0;
            Random random = new Random(seed);
            AttributesAgent attributesAgent = new AttributesAgent(i);

            Pedestrian currentPedestrian = new Pedestrian(attributesAgent, random);
            pedestrians.add(currentPedestrian);
        }

        return pedestrians;
    }

    private List<Stimulus> createElapsedTimeStimuli(int totalStimuli) {
        List<Stimulus> elapsedTimeStimuli = new ArrayList<>();

        for (int i = 0; i < totalStimuli; i++) {
            double time = 1.0;

            ElapsedTime currentElapsedTime = new ElapsedTime(time);
            elapsedTimeStimuli.add(currentElapsedTime);
        }

        return elapsedTimeStimuli;
    }

    private Topography createTopography() {
        Topography topography = new Topography();

        return topography;
    }

    private ArrayList<Target> createTwoTargets() {
        ArrayList<Target> targets = new ArrayList<>();

        Target target1 = createTarget(new VPoint(0, 0), 5, 1);
        Target target2 = createTarget(new VPoint(5, 0), 5, 2);

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

    @Test
    public void updateSetsElapsedTimeIfNoOtherStimulusIsPresent() {
        Topography topography = createTopography();

        List<Pedestrian> pedestrians = createPedestrians(2);
        List<Stimulus> stimuli = createElapsedTimeStimuli(1);

        SimplePerceptionModel simplePerceptionModel = new SimplePerceptionModel();
        simplePerceptionModel.initialize(topography,0);

        pedestrians.forEach(pedestrian -> assertNull(pedestrian.getMostImportantStimulus()));

        simplePerceptionModel.update(getPedSpecififStimuli(pedestrians, stimuli));

        Stimulus expectedStimulus = stimuli.get(0);
        pedestrians.forEach(pedestrian -> assertTrue(expectedStimulus == pedestrian.getMostImportantStimulus()));
    }

    @Test
    public void updateRanksChangeTargetScriptedHigherThanElapsedTime() {
        Topography topography = createTopography();

        List<Pedestrian> pedestrians = createPedestrians(2);
        List<Stimulus> stimuli = new ArrayList<>();

        Stimulus expectedStimulus = new ChangeTargetScripted();
        stimuli.add(new ElapsedTime());
        stimuli.add(expectedStimulus);

        SimplePerceptionModel simplePerceptionModel = new SimplePerceptionModel();
        simplePerceptionModel.initialize(topography, 0);

        pedestrians.forEach(pedestrian -> assertNull(pedestrian.getMostImportantStimulus()));

        simplePerceptionModel.update(getPedSpecififStimuli(pedestrians, stimuli));

        // Use "==" to compare if it is the same reference!
        pedestrians.forEach(pedestrian -> assertTrue(expectedStimulus == pedestrian.getMostImportantStimulus()));
    }

    @Test
    public void updateRanksChangeTargetHigherThanElapsedTime() {
        Topography topography = createTopography();

        List<Pedestrian> pedestrians = createPedestrians(2);
        List<Stimulus> stimuli = new ArrayList<>();

        Stimulus expectedStimulus = new ChangeTarget();
        stimuli.add(new ElapsedTime());
        stimuli.add(expectedStimulus);

        SimplePerceptionModel simplePerceptionModel = new SimplePerceptionModel();
        simplePerceptionModel.initialize(topography, 0);

        pedestrians.forEach(pedestrian -> assertNull(pedestrian.getMostImportantStimulus()));

        simplePerceptionModel.update(getPedSpecififStimuli(pedestrians, stimuli));

        // Use "==" to compare if it is the same reference!
        pedestrians.forEach(pedestrian -> assertTrue(expectedStimulus == pedestrian.getMostImportantStimulus()));
    }

    @Test
    public void updateRanksThreatHigherThanElapsedTime() {
        Topography topography = createTopography();
        Target target = createTarget(new VPoint(0, 0), 5, 1);
        topography.addTarget(target);

        double expectedTime = 0.123;
        Stimulus expectedStimulus = new Threat(expectedTime, target.getId());

        List<Pedestrian> pedestrians = createPedestrians(2);
        List<Stimulus> stimuli = new ArrayList<>();

        stimuli.add(new ElapsedTime());
        stimuli.add(expectedStimulus);

        SimplePerceptionModel simplePerceptionModel = new SimplePerceptionModel();
        simplePerceptionModel.initialize(topography, 0);

        pedestrians.forEach(pedestrian -> assertNull(pedestrian.getMostImportantStimulus()));

        simplePerceptionModel.update(getPedSpecififStimuli(pedestrians, stimuli));

        pedestrians.forEach(pedestrian -> assertTrue(expectedStimulus.getTime() == pedestrian.getMostImportantStimulus().getTime()));
    }


    @Test
    public void updateRanksWaitHigherThanElapsedTime() {
        Topography topography = createTopography();

        List<Pedestrian> pedestrians = createPedestrians(2);
        List<Stimulus> stimuli = new ArrayList<>();

        Stimulus expectedStimulus = new Wait();
        stimuli.add(new ElapsedTime());
        stimuli.add(expectedStimulus);

        SimplePerceptionModel simplePerceptionModel = new SimplePerceptionModel();
        simplePerceptionModel.initialize(topography,0);

        pedestrians.forEach(pedestrian -> assertNull(pedestrian.getMostImportantStimulus()));

        simplePerceptionModel.update(getPedSpecififStimuli(pedestrians, stimuli));

        // Use "==" to compare if it is the same reference!
        pedestrians.forEach(pedestrian -> assertTrue(expectedStimulus == pedestrian.getMostImportantStimulus()));
    }



    @Test
    public void updateRanksChangeTargetScriptedHigherThanChangeTarget() {
        Topography topography = createTopography();

        List<Pedestrian> pedestrians = createPedestrians(2);
        List<Stimulus> stimuli = new ArrayList<>();

        Stimulus expectedStimulus = new ChangeTargetScripted();
        stimuli.add(new ElapsedTime());
        stimuli.add(new ChangeTarget());
        stimuli.add(expectedStimulus);

        SimplePerceptionModel simplePerceptionModel = new SimplePerceptionModel();
        simplePerceptionModel.initialize(topography, 0);

        pedestrians.forEach(pedestrian -> assertNull(pedestrian.getMostImportantStimulus()));

        simplePerceptionModel.update(getPedSpecififStimuli(pedestrians, stimuli));

        // Use "==" to compare if it is the same reference!
        pedestrians.forEach(pedestrian -> assertTrue(expectedStimulus == pedestrian.getMostImportantStimulus()));
    }

    private HashMap<Pedestrian, List<Stimulus>> getPedSpecififStimuli(List<Pedestrian> pedestrians, List<Stimulus> stimuli){
        HashMap<Pedestrian, List<Stimulus>> pedSpecififStimuli = new HashMap<Pedestrian, List<Stimulus>>();
        for (Pedestrian pedestrian : pedestrians){
            pedSpecififStimuli.put(pedestrian, stimuli);
        }
        return pedSpecififStimuli;
    }
}