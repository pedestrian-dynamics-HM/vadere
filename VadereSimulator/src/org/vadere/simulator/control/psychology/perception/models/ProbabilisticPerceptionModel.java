package org.vadere.simulator.control.psychology.perception.models;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.psychology.information.InformationState;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ProbabilisticPerceptionModel extends PerceptionModel {

    private static Logger logger = Logger.getLogger(ProbabilisticPerceptionModel.class);

    HashMap<Pedestrian, List<Stimulus>> processedStimuli;
    private RandomGenerator rng;

    @Override
    public void initialize(Topography topography, final double simTimeStepLengh) {
        rng = new JDKRandomGenerator(0); //TODO: use seed from json?
        processedStimuli = new HashMap<>();
    }

    @Override
    public void update(HashMap<Pedestrian, List<Stimulus>> pedSpecificStimuli) {


        for (Map.Entry<Pedestrian, List<Stimulus>> entry : pedSpecificStimuli.entrySet()) {

            Stimulus mostImportantStimulus;
            if (isStimulusNew(entry.getValue(), entry.getKey())) {
                mostImportantStimulus = getMostImportantStimulusFromProbabilites(entry.getValue(), entry.getKey());
                logger.info("Pedestrian with id=" + entry.getKey().getId() + " got new Stimulus " + mostImportantStimulus.toString());
            }
            else {
                mostImportantStimulus = getStimulusExisting(pedSpecificStimuli, entry.getKey());
                logger.info("Pedestrian with id=" + entry.getKey().getId() + " has most important stimulus " + mostImportantStimulus.toString());
            }
            entry.getKey().setMostImportantStimulus(mostImportantStimulus);
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

    private Stimulus getMostImportantStimulusFromProbabilites(List<Stimulus> stimuli, Pedestrian ped) {

        Stimulus mostImportantStimulus = stimuli.stream()
                .filter(stimulus -> stimulus instanceof ElapsedTime)
                .collect(Collectors.toList())
                .get(0);

        List<Stimulus> externalStimuli = stimuli.stream()
                .filter(stimulus -> !(stimulus instanceof ElapsedTime))
                .collect(Collectors.toList());

        double sumOfProbsExternalStimuli = externalStimuli.stream().map(Stimulus::getPerceptionProbability).reduce(0.0, Double::sum);
        double probRemaining = 1.0 - sumOfProbsExternalStimuli;

        if (sumOfProbsExternalStimuli > 1.0){
            throw new IllegalArgumentException("The sum of probabilites = " + sumOfProbsExternalStimuli +". This exceeds 1.0");
        }


        Stimulus re = drawStimulusFromRandomDistribution(stimuli, mostImportantStimulus, externalStimuli, probRemaining);

        if ((sumOfProbsExternalStimuli > 0.0) && (re instanceof ElapsedTime)){
            ped.getKnowledgeBase().setInformationState(InformationState.INFORMATION_UNCONVINCING_RECEIVED);
        }

        return re;
    }

    private Stimulus drawStimulusFromRandomDistribution(final List<Stimulus> stimuli, final Stimulus mostImportantStimulus, final List<Stimulus> externalStimuli, final double probRemaining) {
        List<Integer> stimuliIndex = IntStream.range(0, stimuli.size()).boxed().collect(Collectors.toList());
        List<Double> probs = externalStimuli.stream().map(Stimulus::getPerceptionProbability).collect(Collectors.toList());

        // add motivation = ElapsedTime Stimulus with adapted probability to stimuli list
        probs.add(probRemaining);
        externalStimuli.add(mostImportantStimulus);

        EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(rng, stimuliIndex.stream().mapToInt(i -> i).toArray(), probs.stream().mapToDouble(i -> i).toArray());
        return externalStimuli.get(dist.sample());
    }


    private boolean isStimulusNew(final List<Stimulus> stimuli, final Pedestrian pedestrian) {

        if (getProcessedStimuli().containsKey(pedestrian)){
            List<Stimulus> oldStimuli = getProcessedStimuli().get(pedestrian);
            return !oldStimuli.equals(stimuli);
        }

        return true;
    }





}
