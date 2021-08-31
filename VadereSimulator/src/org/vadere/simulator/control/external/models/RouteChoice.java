package org.vadere.simulator.control.external.models;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vadere.simulator.control.external.reaction.ReactionModel;
import org.vadere.simulator.models.bhm.PedestrianBHM;
import org.vadere.simulator.models.osm.OptimalStepsModel;
import org.vadere.simulator.models.osm.PedestrianOSM;
import org.vadere.state.psychology.information.InformationState;
import org.vadere.state.psychology.perception.types.ChangeTarget;
import org.vadere.state.scenario.Pedestrian;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class RouteChoice extends ControlModel {

    private JSONObject command;
    private Random random;

    public RouteChoice() {
        super();
        random = new Random(0);
    }


    @Override
    protected void generateStimulusforPed(Pedestrian ped, JSONObject pedCommand, int commandId) {

        command = pedCommand;
        // get information from controller
        final Pair<LinkedList<Integer>, Double> targetAndProb = getTargetFromNavigationApp();

        double prob = targetAndProb.getValue();
        LinkedList<Integer> targets = targetAndProb.getKey();

        double timeCommandExecuted;
        timeCommandExecuted = this.simTime + getSimTimeStepLength();
        ped.getKnowledgeBase().setInformationState(InformationState.INFORMATION_RECEIVED);
        this.stimulusController.setDynamicStimulus(ped, new ChangeTarget(timeCommandExecuted, prob, targets, commandId), timeCommandExecuted);
        logger.debug("Pedestrian " + ped.getId() + ": created Stimulus ChangeTarget. New target list " + targets);

    }


    private Pair<LinkedList<Integer>, Double> getTargetFromNavigationApp() {

        LinkedList<Integer> targets = readTargetsFromJson();
        LinkedList<Double> probs = readProbabilitiesFromJson();
        LinkedList<Double> reactionsProbs = readReactionProbabilitiesFromJson();

        EnumeratedIntegerDistribution dist = getDiscreteDistribution(targets, probs);
        int newTargetId = dist.sample();

        LinkedList<Integer> nextTarget = new LinkedList<>();
        nextTarget.add(newTargetId);
        double probability =  reactionsProbs.get(targets.indexOf(newTargetId));

        return new Pair<>(nextTarget, probability);

    }

    private EnumeratedIntegerDistribution getDiscreteDistribution(LinkedList<Integer> possibleTargets, LinkedList<Double> probs){


        RandomGenerator rng = new JDKRandomGenerator(random.nextInt());

        return new EnumeratedIntegerDistribution(rng,
                possibleTargets.stream().mapToInt(i -> i).toArray(),
                probs.stream().mapToDouble(i -> i).toArray());
    }


    private LinkedList<Integer> readTargetsFromJson() {
        LinkedList<Integer> targets = new LinkedList<>();
        JSONArray targetList = (JSONArray) command.get("targetIds");

        for (Object t : targetList) {
            targets.add((Integer) t);
        }
        return targets;
    }

    private LinkedList<Double> readProbabilitiesFromJson() {
        LinkedList<Double> probs = new LinkedList<>();
        JSONArray targetList = (JSONArray) command.get("probability");

        for (int i = 0; i < targetList.length(); i++) {
            probs.add(targetList.getDouble(i));
        }
        return probs;
    }

    private LinkedList<Double> readReactionProbabilitiesFromJson() {
        LinkedList<Double> probs = new LinkedList<>();

        JSONArray targetList = (JSONArray) command.get("reactionProbability");

        for (int i = 0; i < targetList.length(); i++) {
            probs.add(targetList.getDouble(i));
        }

        return probs;
    }


}
