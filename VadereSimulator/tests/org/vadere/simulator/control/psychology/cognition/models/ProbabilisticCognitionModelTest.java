package org.vadere.simulator.control.psychology.cognition.models;


import org.apache.commons.math3.util.Precision;
import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.perception.types.ChangeTarget;
import org.vadere.state.psychology.perception.types.ElapsedTime;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.psychology.perception.types.Wait;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;


import java.util.*;

import static org.junit.Assert.*;

public class ProbabilisticCognitionModelTest {



    private static double ALLOWED_DOUBLE_ERROR = 10e-3;

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



    @Test
    public void updateSetsElapsedTimeIfNoOtherStimulusIsPresent() {
        double presicison = 0.025; // percentage error

        Topography topography = createTopography();
        double time = 0.0;
        int sampleSize = 10000;

        double prob2Is = 0.4;
        double prob3Is = 0.2;
        double prob1Is = 1.0-prob2Is-prob3Is;

        List<Pedestrian> pedestrians = createPedestrians(sampleSize);
        LinkedList<Stimulus> stimuli = new LinkedList<>();
        stimuli.add(new ElapsedTime(time));
        stimuli.add(new ChangeTarget(time, prob2Is));
        stimuli.add(new Wait(time, prob3Is));

        ProbabilisticCognitionModel probabilisticPerceptionModel = new ProbabilisticCognitionModel();
        probabilisticPerceptionModel.initialize(topography);

        for (Pedestrian pedestrian : pedestrians){
            pedestrian.setPerceivedStimuli(new LinkedList<>());
            pedestrian.setNextPerceivedStimuli(stimuli);
        }


        probabilisticPerceptionModel.update(pedestrians);
        double prob1 = 1.0 * pedestrians.stream().filter(ped -> ped.getMostImportantStimulus() instanceof ElapsedTime).count() / sampleSize;
        double prob2 = 1.0 * pedestrians.stream().filter(ped -> ped.getMostImportantStimulus() instanceof ChangeTarget).count() / sampleSize;
        double prob3 = 1.0 * pedestrians.stream().filter(ped -> ped.getMostImportantStimulus() instanceof Wait).count() / sampleSize;

        assertTrue( Precision.equals(prob1, prob1Is, presicison));
        assertTrue( Precision.equals(prob2, prob2Is, presicison));
        assertTrue( Precision.equals(prob3, prob3Is, presicison));

    }

    @Test
    public void testCommandIdMissingWrapper() {
        try {
            wrongProbs(0.9);
        } catch (final IllegalArgumentException e) {
            assertTrue(e.getMessage().equals("The sum of probabilites = 1.3. This exceeds 1.0"));
        }
    }


    public void wrongProbs(double prob3Is) {

        Topography topography = createTopography();
        double time = 0.0;
        int sampleSize = 10000;

        double prob2Is = 0.4;

        List<Pedestrian> pedestrians = createPedestrians(sampleSize);
        LinkedList<Stimulus> stimuli = new LinkedList<>();
        stimuli.add(new ElapsedTime(time));
        stimuli.add(new ChangeTarget(time, prob2Is));
        stimuli.add(new Wait(time, prob3Is));


        ProbabilisticCognitionModel probabilisticPerceptionModel = new ProbabilisticCognitionModel();

        probabilisticPerceptionModel.initialize(topography);
        pedestrians.forEach(pedestrian -> assertNull(pedestrian.getMostImportantStimulus()));

        updateStimuli(pedestrians, probabilisticPerceptionModel, stimuli);

    }

    @Test
    public void testRecurring() {

        Topography topography = createTopography();
        List<Pedestrian> pedestrians = createPedestrians(1);

        ProbabilisticCognitionModel probabilisticPerceptionModel = new ProbabilisticCognitionModel();
        probabilisticPerceptionModel.initialize(topography);
        pedestrians.forEach(pedestrian -> assertNull(pedestrian.getMostImportantStimulus()));


        double time = 0.0;
        double time1 = 0.4;
        double time2 = 0.8;
        double time3 = 1.2;
        double time4 = 1.4;

        LinkedList<Stimulus> stimuli = new LinkedList<>();
        stimuli.add(new ElapsedTime(time));
        stimuli.add(new ChangeTarget(time, 0));
        stimuli.add(new Wait(time, 0));

        LinkedList<Stimulus> stimuli1 = new LinkedList<>();
        stimuli1.add(new ElapsedTime(time1));
        stimuli1.add(new ChangeTarget(time1, 0));
        stimuli1.add(new Wait(time1, 0));


        LinkedList<Stimulus> stimuli2 =new LinkedList<>();
        stimuli2.add(new ElapsedTime(time2));
        stimuli2.add(new ChangeTarget(time2, 0));
        stimuli2.add(new Wait(time2, 1));

        LinkedList<Stimulus> stimuli3 = new LinkedList<>();
        stimuli3.add(new ElapsedTime(time3));
        stimuli3.add(new ChangeTarget(time3, 0));
        stimuli3.add(new Wait(time3, 1));

        LinkedList<Stimulus> stimuli4 = new LinkedList<>();
        stimuli4.add(new ElapsedTime(time4));
        stimuli4.add(new ChangeTarget(time4, 0));
        stimuli4.add(new Wait(time4, 1));

        updateStimuli(pedestrians, probabilisticPerceptionModel, stimuli);
        pedestrians.forEach(pedestrian -> assertTrue(pedestrian.getMostImportantStimulus() instanceof ElapsedTime));
        updateStimuli(pedestrians, probabilisticPerceptionModel, stimuli1);
        pedestrians.forEach(pedestrian -> assertTrue(pedestrian.getMostImportantStimulus() instanceof ElapsedTime));

        updateStimuli(pedestrians, probabilisticPerceptionModel, stimuli2);
        pedestrians.forEach(pedestrian -> assertTrue(pedestrian.getMostImportantStimulus() instanceof Wait));
        updateStimuli(pedestrians, probabilisticPerceptionModel, stimuli3);
        pedestrians.forEach(pedestrian -> assertTrue(pedestrian.getMostImportantStimulus() instanceof Wait));
        updateStimuli(pedestrians, probabilisticPerceptionModel, stimuli4);
        pedestrians.forEach(pedestrian -> assertTrue(pedestrian.getMostImportantStimulus() instanceof Wait));



    }

    private void updateStimuli(final List<Pedestrian> pedestrians, final ProbabilisticCognitionModel probabilisticPerceptionModel, final LinkedList<Stimulus> stimuli) {
        for (Pedestrian pedestrian : pedestrians){
            pedestrian.setPerceivedStimuli(new LinkedList<>());
            pedestrian.setNextPerceivedStimuli(stimuli);
        }

        probabilisticPerceptionModel.update(pedestrians);
    }


}
