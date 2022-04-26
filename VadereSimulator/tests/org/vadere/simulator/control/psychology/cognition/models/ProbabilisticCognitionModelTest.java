package org.vadere.simulator.control.psychology.cognition.models;


import org.apache.commons.math3.util.Precision;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.psychology.AttributesProbabilisticCognitionModel;
import org.vadere.state.attributes.models.psychology.HelperAttributes.AttributesRouteChoiceDefinition;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;


import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;


public class ProbabilisticCognitionModelTest {

    private List<Attributes> attributes;

    private AttributesProbabilisticCognitionModel attributesProbabilisticCognitionModel;
    private String instruction  = "take target 2";


    private static double ALLOWED_DOUBLE_ERROR = 10e-3;

    @Before
    public void initializeReactionBehavior(){


        AttributesRouteChoiceDefinition attr1 = getRouteChoiceDefinition(1,2,0.1, 0.9, "A");
        AttributesRouteChoiceDefinition attr2 = getRouteChoiceDefinition(3,4,0.5, 0.5, "B");

        AttributesProbabilisticCognitionModel probModelAttr = new AttributesProbabilisticCognitionModel();
        probModelAttr.getRouteChoices().add(attr1);
        probModelAttr.getRouteChoices().add(attr2);
        this.attributesProbabilisticCognitionModel = probModelAttr;

        this.attributes = new LinkedList<>();
        this.attributes.add(probModelAttr);
        
    }

    @NotNull
    private AttributesRouteChoiceDefinition getRouteChoiceDefinition(int targetId1, int targetId2, double prob1, double prob2, String instruction) {
        AttributesRouteChoiceDefinition attr = new AttributesRouteChoiceDefinition();

        LinkedList<Integer> routeIds = new LinkedList<>();
        routeIds.add(targetId1);
        routeIds.add(targetId2);
        attr.setTargetIds(routeIds);

        LinkedList<Double> routeProbs = new LinkedList<>();
        routeProbs.add(prob1);
        routeProbs.add(prob2);
        attr.setTargetProbabilities(routeProbs);

        attr.setInstruction(instruction);
        return attr;
    }

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
        Topography topography = mock(Topography.class, Mockito.RETURNS_DEEP_STUBS);

        List<Integer> routeIds = new LinkedList<>();
        routeIds.add(1);
        routeIds.add(2);
        routeIds.add(3);
        routeIds.add(4);

        Mockito.when(topography.getTargetIds()).thenReturn(routeIds);

        return topography;
    }



    @Test
    public void updateSetsElapsedTimeIfNoOtherStimulusIsPresent() {
        double presicison = 0.025; // percentage error

        Topography topography = createTopography();
        int sampleSize = 10000;

        AttributesProbabilisticCognitionModel attr = this.attributesProbabilisticCognitionModel;


        List<Pedestrian> pedestrians = createPedestrians(sampleSize);
        pedestrians.stream().forEach(ped -> ped.setMostImportantStimulus(new InformationStimulus("A")));

        ProbabilisticCognitionModel probabilisticPerceptionModel = new ProbabilisticCognitionModel();
        probabilisticPerceptionModel.initialize(topography, attributes, new Random(0));



        LinkedList<Integer> targetIds = probabilisticPerceptionModel.getFilteredAttributes("A").getTargetIds();
        LinkedList<Double> targetProbs = probabilisticPerceptionModel.getFilteredAttributes("A").getTargetProbabilities();



        probabilisticPerceptionModel.update(pedestrians);
        double prob1Is = 1.0 * pedestrians.stream().filter(ped -> ped.getTargets().getFirst() == targetIds.get(0)).count() / sampleSize;
        double prob2Is = 1.0 * pedestrians.stream().filter(ped -> ped.getTargets().getFirst() == targetIds.get(1)).count() / sampleSize;

        assertTrue( Precision.equals(targetProbs.get(0), prob1Is, presicison));
        assertTrue( Precision.equals(targetProbs.get(1), prob2Is, presicison));

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

        probabilisticPerceptionModel.initialize(topography, attributes, new Random(0));
        pedestrians.forEach(pedestrian -> assertNull(pedestrian.getMostImportantStimulus()));

        updateStimuli(pedestrians, probabilisticPerceptionModel, stimuli);

    }

    @Test
    public void testRecurring() {

        Topography topography = createTopography();
        List<Pedestrian> pedestrians = createPedestrians(1);

        ProbabilisticCognitionModel probabilisticPerceptionModel = new ProbabilisticCognitionModel();
        probabilisticPerceptionModel.initialize(topography, attributes, new Random(0));
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
