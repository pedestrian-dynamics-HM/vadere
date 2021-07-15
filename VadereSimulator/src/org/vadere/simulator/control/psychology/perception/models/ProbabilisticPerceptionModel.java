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
                logger.info("Pedestrian with id=" + pedestrian.getId() + " got new Stimulus " + pedestrian.getMostImportantStimulus());
            }
            else {
                logger.info("Pedestrian with id=" + pedestrian.getId() + " has most important stimulus " + pedestrian.getMostImportantStimulus());
            }
        }
        setProcessedStimuli(pedSpecificStimuli);
    }


    public void setProcessedStimuli(final HashMap<Pedestrian, List<Stimulus>> processedStimuli) {
        this.processedStimuli = processedStimuli;
    }

    public HashMap<Pedestrian, List<Stimulus>> getProcessedStimuli() {
        return processedStimuli;
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

        if (sumOfProbsExternalStimuli > 1.0){
            throw new IllegalArgumentException("The sum of probabilites = " + sumOfProbsExternalStimuli +". This exceeds 1.0");
        }


        double probRemaining = 1.0 - sumOfProbsExternalStimuli;
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


        if (getProcessedStimuli().containsKey(pedestrian)){

            List<Stimulus> oldStimuli = getProcessedStimuli().get(pedestrian).stream()
                    .filter(stimulus -> !(stimulus instanceof ElapsedTime))
                    .collect(Collectors.toList());

            List<Stimulus> newStimuli = stimuli.stream()
                    .filter(stimulus -> !(stimulus instanceof ElapsedTime))
                    .collect(Collectors.toList());
            

            if (oldStimuli.equals(newStimuli)) {
                return false;
            }
            else{
                for (Stimulus s : oldStimuli) {
                   s.setTime(s.getTime()+0.4);
                }
                if (oldStimuli.equals(newStimuli)){
                    logger.info("was here");
                    return false;
                }


            }
        }

        return true;
    }

}
