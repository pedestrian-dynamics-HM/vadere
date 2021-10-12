package org.vadere.simulator.control.psychology.cognition.models;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.simulator.control.psychology.cognition.models.ICognitionModel;
import org.vadere.state.psychology.information.InformationState;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ProbabilisticCognitionModel extends ICognitionModel {

    private static Logger logger = Logger.getLogger(ProbabilisticCognitionModel.class);

    private RandomGenerator rng;

    @Override
    public void initialize(Topography topography) {
        rng = new JDKRandomGenerator(0); //TODO: use seed from json?
        processedStimuli = new HashMap<>();
    }

    @Override
    public void update(Collection<Pedestrian> pedestrians) {

        for (Map.Entry<Pedestrian, List<Stimulus>> pedStimuli : pedSpecificStimuli.entrySet()) {

            List<Stimulus> stimuli = pedStimuli.getValue();
            Pedestrian ped = pedStimuli.getKey();

            if (isStimuliRandomDistributionNew(stimuli, ped)) {
                Stimulus mostImportantStimulus = drawStimulusFromRandomDistribution(getMostImportStimulus(stimuli), getExternalStimuli(stimuli));
                ped.setMostImportantStimulus(mostImportantStimulus);
                setInformationState(ped, mostImportantStimulus,stimuli);
            } else {
                updateTimeOfMostImportantStimulus(pedSpecificStimuli, ped);
            }
            setInformationStateGroupMember(ped.getPedGroupMembers());
        }
        setProcessedStimuli(pedSpecificStimuli);
    }




    protected void setInformationState(Pedestrian pedestrian, Stimulus mostImportantStimulus, List<Stimulus> stimuli) {
        // Each external stimulus has an acceptance rate defined
        // If an agent accepts an external stimulus, the internal stimulus ElapsedTime is replaced by that external stimulus.
        // If there are external stimuli, but the internal stimulus (ElapsedTime) is still present,
        // we assume that the agent does not respond to that external stimulus, because they think is is not important for them.
        // In this case, the information is not convincing.
        if ((mostImportantStimulus instanceof ElapsedTime) && (getExternalStimuli(stimuli).size() > 0)){
            pedestrian.getKnowledgeBase().setInformationState(InformationState.INFORMATION_UNCONVINCING_RECEIVED);
        }
        else{
            pedestrian.getKnowledgeBase().setInformationState(InformationState.INFORMATION_CONVINCING_RECEIVED);
        }
    }

    // private


    private Stimulus drawStimulusFromRandomDistribution(Stimulus elapsedTimeStimulus, final List<Stimulus> externalStimuli) {

        checkIfProbabiliesValid(externalStimuli);

        // collect possible stimuli in a list: [stimulus 1,     stimulus 2, ...     stimulus n,     ElapsedTimeStimulus]
        List<Stimulus> stimuli = externalStimuli.stream().collect(Collectors.toList());
        stimuli.add(elapsedTimeStimulus);

        // collect assigned probabilities in a list [probability 1   probability 2 ...   probability n   1 - sum(..9    ]
        List<Double> acceptanceDistribution = externalStimuli.stream().map(Stimulus::getPerceptionProbability).collect(Collectors.toList());
        acceptanceDistribution.add(1 - getSumOfProbsExternalStimuli(externalStimuli));

        // setup distribution and draw index
        EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(rng,
                IntStream.range(0, stimuli.size()).boxed().collect(Collectors.toList()).stream().mapToInt(i -> i).toArray(),
                acceptanceDistribution.stream().mapToDouble(i -> i).toArray());

        int indexDrawn = dist.sample();
        return stimuli.get(indexDrawn);
    }




    private List<Stimulus> getExternalStimuli(final List<Stimulus> stimuli) {
        return stimuli.stream()
                .filter(stimulus -> !(stimulus instanceof ElapsedTime))
                .collect(Collectors.toList());
    }

    private Stimulus getMostImportStimulus(final List<Stimulus> stimuli) {
        return stimuli.stream()
                .filter(stimulus -> stimulus instanceof ElapsedTime)
                .collect(Collectors.toList())
                .get(0);
    }

    private void checkIfProbabiliesValid(final List<Stimulus> externalStimuli) {

        double sumOfProbsExternalStimuli = getSumOfProbsExternalStimuli(externalStimuli);
        if (sumOfProbsExternalStimuli > 1.0){
            throw new IllegalArgumentException("The sum of probabilites = " + sumOfProbsExternalStimuli +". This exceeds 1.0");
        }
    }

    private double getSumOfProbsExternalStimuli(final List<Stimulus> externalStimuli) {
        return externalStimuli.stream().map(Stimulus::getPerceptionProbability).reduce(0.0, Double::sum);
    }






}
