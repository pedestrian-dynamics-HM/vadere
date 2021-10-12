package org.vadere.simulator.control.psychology.cognition.models;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.information.InformationState;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootstepHistory;
import org.vadere.util.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ProbabilisticCognitionModel extends AProbabilisticModel {

    private static Logger logger = Logger.getLogger(ProbabilisticCognitionModel.class);

    private RandomGenerator rng;

    @Override
    public void initialize(Topography topography) {
        rng = new JDKRandomGenerator(0); //TODO: use seed from json?
    }

    @Override
    public void update(Collection<Pedestrian> pedestrians) {

        for (Pedestrian pedestrian : pedestrians) {

            LinkedList<Stimulus> oldStimuli = pedestrian.getPerceivedStimuli();
            LinkedList<Stimulus> newStimuli = pedestrian.getNextPerceivedStimuli();

            if (!oldStimuli.equals(newStimuli)) {
                Stimulus mostImportantStimulus = drawStimulusFromRandomDistribution(newStimuli);
                pedestrian.setMostImportantStimulus(mostImportantStimulus);
                setInformationState(pedestrian, mostImportantStimulus,newStimuli);
                applyAction(pedestrian);
            }
            pedestrian.setNextPerceivedStimuli(newStimuli);
            setInformationStateGroupMember(pedestrian.getPedGroupMembers());
        }
    }

    protected boolean pedestrianCannotMove(Pedestrian pedestrian) {
        boolean cannotMove = false;

        FootstepHistory footstepHistory = pedestrian.getFootstepHistory();
        int requiredFootSteps = 2;

        if (footstepHistory.size() >= requiredFootSteps
                && footstepHistory.getAverageSpeedInMeterPerSecond() <= 0.05) {
            cannotMove = true;
        }

        return cannotMove;
    }

    protected void applyAction(Pedestrian pedestrian){
        if (pedestrianCannotMove(pedestrian)) {
            pedestrian.setSelfCategory(SelfCategory.COOPERATIVE);
        } else {
            // in the super class CooperativeCognitionModel, there is no differentiation between sub-behaviors.
            Stimulus stimulus = pedestrian.getMostImportantStimulus();
            SelfCategory nextSelfCategory;

            if (stimulus instanceof ChangeTarget) {
                nextSelfCategory = SelfCategory.CHANGE_TARGET;
                // make sure that the target is set directly
                // this is necessary, because the target is set in the update scheme only, it the agent takes a step
                // if agents are not in the event queue or their timeOfNextStep > simeTimeStep, the target will not be set properly in the locomotion layer
                // In the {@link ChangeTargetScriptedCognitionModel}, the target is also set twice.
                ChangeTarget changeTarget = (ChangeTarget) pedestrian.getMostImportantStimulus();
                pedestrian.setTargets(changeTarget.getNewTargetIds());
                pedestrian.setNextTargetListIndex(0);

            } else if (stimulus instanceof Threat) {
                nextSelfCategory = SelfCategory.THREATENED;
            } else if (stimulus instanceof Wait || stimulus instanceof WaitInArea) {
                nextSelfCategory = SelfCategory.WAIT;
            } else if (stimulus instanceof ElapsedTime) {
                nextSelfCategory = SelfCategory.TARGET_ORIENTED;
            } else {
                throw new IllegalArgumentException(String.format("Stimulus \"%s\" not supported by \"%s\"",
                        stimulus.getClass().getSimpleName(),
                        this.getClass().getSimpleName()));
            }

            pedestrian.setSelfCategory(nextSelfCategory);

        }
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


    private Stimulus drawStimulusFromRandomDistribution(List<Stimulus> newStimuli) {
        Stimulus elapsedTimeStimulus = newStimuli.stream().filter(stimulus -> stimulus instanceof ElapsedTime).findFirst().orElseThrow();
        List<Stimulus> externalStimuli = newStimuli.stream().filter(stimulus -> !(stimulus instanceof ElapsedTime)).collect(Collectors.toList());
        checkIfProbabiliesValid(externalStimuli);

        // collect possible stimuli in a list: [stimulus 1,     stimulus 2, ...     stimulus n,     ElapsedTimeStimulus]
        List<Stimulus> stimuli = externalStimuli;


        // collect assigned probabilities in a list [probability 1   probability 2 ...   probability n   1 - sum(..9    ]
        List<Double> acceptanceDistribution = externalStimuli.stream().map(Stimulus::getPerceptionProbability).collect(Collectors.toList());

        acceptanceDistribution.add(1 - getSumOfProbsExternalStimuli(externalStimuli));
        stimuli.add(elapsedTimeStimulus);

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
