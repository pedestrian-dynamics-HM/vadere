package org.vadere.simulator.control.psychology.perception.models;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ProbabilisticPerceptionModel implements IPerceptionModel {

    private Topography topography;
    private RandomGenerator rng;

    @Override
    public void initialize(Topography topography) {
        this.topography = topography;
        rng = new JDKRandomGenerator(new Random().nextInt());

    }

    @Override
    public void update(HashMap<Pedestrian, List<Stimulus>> pedSpecificStimuli) {
        for (Pedestrian pedestrian : pedSpecificStimuli.keySet()) {
            Stimulus mostImportantStimulus = getMostImportantStimulusFromProbabilites(pedSpecificStimuli.get(pedestrian), pedestrian);
            pedestrian.setMostImportantStimulus(mostImportantStimulus);
        }
    }

    public void update(Collection<Pedestrian> pedestrians, List<Stimulus> generalStimuli) {
        for (Pedestrian pedestrian : pedestrians) {
            Stimulus mostImportantStimulus = getMostImportantStimulusFromProbabilites(generalStimuli, pedestrian);
            pedestrian.setMostImportantStimulus(mostImportantStimulus);
        }
    }


    private Stimulus getMostImportantStimulusFromProbabilites(List<Stimulus> stimuli, Pedestrian pedestrian) {


        Stimulus innerStimulus = stimuli.stream()
                .filter(stimulus -> stimulus instanceof ElapsedTime)
                .collect(Collectors.toList())
                .get(0);

        List<Stimulus> externalStimuli = stimuli.stream()
                .filter(stimulus -> !(stimulus instanceof ElapsedTime))
                .collect(Collectors.toList());


        //TODO: discuss -> what to do with space-bound stimuli?
        //Stimulus areaWaitStimulus = selectWaitInAreaContainingPedestrian(pedestrian, stimuli.stream().filter(stimulus -> stimulus instanceof WaitInArea).collect(Collectors.toList()));
        //stimuli = stimuli.stream().filter(stimulus -> !(stimulus instanceof WaitInArea)).collect(Collectors.toList());
        //stimuli.add(areaWaitStimulus);

        double sumOfProbsExternalStimuli = externalStimuli.stream().map(Stimulus::getPerceptionProbability).reduce(0.0, Double::sum);
        double probRemaining = innerStimulus.getPerceptionProbability() - sumOfProbsExternalStimuli;
        innerStimulus.setPerceptionProbability(probRemaining);

        List<Integer> stimuliIndex = IntStream.range(0, stimuli.size())
                .mapToObj(index -> index)
                .collect(Collectors.toList());

        List<Double> probs = stimuli.stream().map(Stimulus::getPerceptionProbability).collect(Collectors.toList());

        EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(rng, stimuliIndex.stream().mapToInt(i -> i).toArray(), probs.stream().mapToDouble(i -> i).toArray());
        return stimuli.get(dist.sample());

    }

    private Stimulus selectWaitInAreaContainingPedestrian(Pedestrian pedestrian, List<Stimulus> waitInAreaStimuli) {
        WaitInArea selectedWaitInArea = null;

        for (Stimulus stimulus : waitInAreaStimuli) {
            WaitInArea waitInArea = (WaitInArea) stimulus;
            boolean pedInArea = waitInArea.getArea().contains(pedestrian.getPosition());

            if (pedInArea) {
                selectedWaitInArea = waitInArea;
            }
        }

        return selectedWaitInArea;
    }
}
