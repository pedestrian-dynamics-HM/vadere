package org.vadere.simulator.control.psychology.cognition.models;


import org.apache.commons.math3.util.Precision;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.psychology.cognition.AttributesProbabilisticCognitionModel;
import org.vadere.state.attributes.models.psychology.cognition.AttributesRouteChoiceDefinition;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;


import java.util.*;

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
        probabilisticPerceptionModel.initialize(topography, new Random(0));
        probabilisticPerceptionModel.setAttributes(attr);



        LinkedList<Integer> targetIds = probabilisticPerceptionModel.getFilteredAttributes("A").getTargetIds();
        LinkedList<Double> targetProbs = probabilisticPerceptionModel.getFilteredAttributes("A").getTargetProbabilities();


        probabilisticPerceptionModel.update(pedestrians);
        double prob1Is = 1.0 * pedestrians.stream().filter(ped -> ped.getTargets().getFirst() == targetIds.get(0)).count() / sampleSize;
        double prob2Is = 1.0 * pedestrians.stream().filter(ped -> ped.getTargets().getFirst() == targetIds.get(1)).count() / sampleSize;

        assertTrue( Precision.equals(targetProbs.get(0), prob1Is, presicison));
        assertTrue( Precision.equals(targetProbs.get(1), prob2Is, presicison));
    }


    @Test
    public void testRecurring() {

        Topography topography = createTopography();
        List<Pedestrian> pedestrians = createPedestrians(1);

        ProbabilisticCognitionModel probabilisticPerceptionModel = new ProbabilisticCognitionModel();
        probabilisticPerceptionModel.initialize(topography, new Random(0));
        probabilisticPerceptionModel.setAttributes(this.attributesProbabilisticCognitionModel);

        pedestrians.stream().forEach(ped -> ped.setMostImportantStimulus(new InformationStimulus("A")));

        probabilisticPerceptionModel.update(pedestrians);
        assertTrue(pedestrians.get(0).getSelfCategory().equals(SelfCategory.CHANGE_TARGET));

        probabilisticPerceptionModel.update(pedestrians);
        assertTrue(pedestrians.get(0).getSelfCategory().equals(SelfCategory.TARGET_ORIENTED));

    }

    //TODO test groups


}
