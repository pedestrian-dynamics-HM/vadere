package org.vadere.simulator.control.psychology.perception.models;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ProbabilisticPerceptionModel implements IPerceptionModel {

    private static Logger logger = Logger.getLogger(ProbabilisticPerceptionModel.class);

    HashMap<Pedestrian, List<Stimulus>> processedStimuli;
    private RandomGenerator rng;

    @Override
    public void initialize(Topography topography) {
        rng = new JDKRandomGenerator(new Random().nextInt());
        processedStimuli = new HashMap<>();
    }

    @Override
    public void update(HashMap<Pedestrian, List<Stimulus>> pedSpecificStimuli) {

        for (Pedestrian pedestrian : pedSpecificStimuli.keySet()) {
            if (isStimulusNew(pedSpecificStimuli.get(pedestrian), pedestrian)) {
                Stimulus mostImportantStimulus = getMostImportantStimulusFromProbabilites(pedSpecificStimuli.get(pedestrian), pedestrian);
                pedestrian.setMostImportantStimulus(mostImportantStimulus);
                logger.info("Pedestrian with id=" + pedestrian.getId() + " got new Stimulus " + pedestrian.getMostImportantStimulus().toString());
            }
            else {
                logger.info("Pedestrian with id=" + pedestrian.getId() + " has most important stimulus " + pedestrian.getMostImportantStimulus().toString());
            }
        }

        this.processedStimuli = pedSpecificStimuli;

    }


    private Stimulus getMostImportantStimulusFromProbabilites(List<Stimulus> stimuli, Pedestrian pedestrian) {

        Stimulus mostImportantStimulus = stimuli.stream()
                .filter(stimulus -> stimulus instanceof ElapsedTime)
                .collect(Collectors.toList())
                .get(0);

        List<Stimulus> externalStimuli = stimuli.stream()
                .filter(stimulus -> !(stimulus instanceof ElapsedTime))
                .collect(Collectors.toList());

        double sumOfProbsExternalStimuli = externalStimuli.stream().map(Stimulus::getPerceptionProbability).reduce(0.0, Double::sum);
        double probRemaining = mostImportantStimulus.getPerceptionProbability() - sumOfProbsExternalStimuli;
        mostImportantStimulus.setPerceptionProbability(probRemaining);

        List<Integer> stimuliIndex = IntStream.range(0, stimuli.size())
                .mapToObj(index -> index)
                .collect(Collectors.toList());

        List<Double> probs = stimuli.stream().map(Stimulus::getPerceptionProbability).collect(Collectors.toList());

        EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(rng, stimuliIndex.stream().mapToInt(i -> i).toArray(), probs.stream().mapToDouble(i -> i).toArray());
        int index = dist.sample();
        mostImportantStimulus = stimuli.get(index);
        return mostImportantStimulus;
    }

    private boolean isStimulusNew(final List<Stimulus> stimuli, final Pedestrian pedestrian) {


        if (processedStimuli.containsKey(pedestrian)){

            List<Stimulus> oldStimuli = processedStimuli.get(pedestrian).stream()
                    .filter(stimulus -> !(stimulus instanceof ElapsedTime))
                    .collect(Collectors.toList());

            List<Stimulus> newStimuli = stimuli.stream()
                    .filter(stimulus -> !(stimulus instanceof ElapsedTime))
                    .collect(Collectors.toList());

            HashSet<Stimulus> stimuli1 = new HashSet<Stimulus>(oldStimuli);
            HashSet<Stimulus> stimuli2 = new HashSet<Stimulus>(newStimuli);

            if (stimuli1.equals(stimuli2)){
                return false;
            }
        }

        return true;
    }

}
