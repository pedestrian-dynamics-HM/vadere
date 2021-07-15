package org.vadere.simulator.control.psychology.perception.models;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.logging.Logger;

import javax.management.RuntimeErrorException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ProbabilisticPerceptionModel implements IPerceptionModel {

    private static Logger logger = Logger.getLogger(ProbabilisticPerceptionModel.class);

    HashMap<Pedestrian, List<Stimulus>> processedStimuli;
    private RandomGenerator rng;
    private double simulationStepLength;

    @Override
    public void initialize(Topography topography, final double simTimeStepLengh) {
        rng = new JDKRandomGenerator(new Random().nextInt());
        processedStimuli = new HashMap<>();
        this.simulationStepLength = simTimeStepLengh;
    }

    @Override
    public void update(HashMap<Pedestrian, List<Stimulus>> pedSpecificStimuli) {

        for (Pedestrian pedestrian : pedSpecificStimuli.keySet()) {
            Stimulus mostImportantStimulus;
            if (isStimulusNew(pedSpecificStimuli.get(pedestrian), pedestrian)) {
                mostImportantStimulus = getMostImportantStimulusFromProbabilites(pedSpecificStimuli.get(pedestrian));
                logger.info("Pedestrian with id=" + pedestrian.getId() + " got new Stimulus " + mostImportantStimulus.toString());
            }
            else {
                mostImportantStimulus = getStimulusExisting(pedSpecificStimuli, pedestrian);
                logger.info("Pedestrian with id=" + pedestrian.getId() + " has most important stimulus " + mostImportantStimulus.toString());
            }
            pedestrian.setMostImportantStimulus(mostImportantStimulus);
        }
        setProcessedStimuli(pedSpecificStimuli);
    }

    private Stimulus getStimulusExisting(final HashMap<Pedestrian, List<Stimulus>> pedSpecificStimuli, final Pedestrian pedestrian) {
        Stimulus mostImportantStimulus = pedestrian.getMostImportantStimulus();
        Stimulus finalMostImportantStimulus = pedestrian.getMostImportantStimulus();
        Collection<Stimulus> stimuli = pedSpecificStimuli.get(pedestrian).stream().filter(stimulus -> stimulus.equals(finalMostImportantStimulus)).collect(Collectors.toList());

        if (stimuli.size() != 1){
            throw new IllegalArgumentException("Multiple or no recurring stimulus found.");
        }

        for (Stimulus s : stimuli){
            mostImportantStimulus = s;
        }
        return mostImportantStimulus;
    }


    public void setProcessedStimuli(final HashMap<Pedestrian, List<Stimulus>> processedStimuli) {
        this.processedStimuli = processedStimuli;
    }

    public HashMap<Pedestrian, List<Stimulus>> getProcessedStimuli() {
        return processedStimuli;
    }

    private Stimulus getMostImportantStimulusFromProbabilites(List<Stimulus> stimuli) {

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

        List<Integer> stimuliIndex = IntStream.range(0, stimuli.size())
                .mapToObj(index -> index)
                .collect(Collectors.toList());

        List<Double> probs = externalStimuli.stream().map(Stimulus::getPerceptionProbability).collect(Collectors.toList());
        probs.add(probRemaining);

        EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(rng, stimuliIndex.stream().mapToInt(i -> i).toArray(), probs.stream().mapToDouble(i -> i).toArray());
        int index = dist.sample();
        externalStimuli.add(mostImportantStimulus);
        mostImportantStimulus = externalStimuli.get(index);
        return mostImportantStimulus;
    }

    private boolean isStimulusNew(final List<Stimulus> stimuli, final Pedestrian pedestrian) {

        if (getProcessedStimuli().containsKey(pedestrian)){

            List<Stimulus> oldStimuli = getProcessedStimuli().get(pedestrian);

            if (oldStimuli.equals(stimuli)){
                return false;
            }
        }

        return true;
    }



}
