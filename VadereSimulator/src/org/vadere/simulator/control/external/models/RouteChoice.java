package org.vadere.simulator.control.external.models;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vadere.state.psychology.information.InformationState;
import org.vadere.state.psychology.perception.types.ChangeTarget;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.scenario.Pedestrian;

import java.util.LinkedList;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RouteChoice extends ControlModel {

    private Random random;

    public RouteChoice() {
        super();
        random = new Random(0);
    }


    @Override
    protected Stimulus addStimulusForPed(Pedestrian ped, JSONObject command, int commandId, double timeCommandExecuted) {

        LinkedList<Double> probs = readProbabilitiesFromJson(command);
        LinkedList<Integer> targets = readTargetsFromJson(command);
        LinkedList<Double> reaction = readReactionProbabilitiesFromJson(command);

        int newTargetindex = getIndexFromRandomDistribution(probs);
        LinkedList<Integer> newTarget = getTargetFromIndex(newTargetindex, targets);
        double acceptanceRate = getReactionProbabilityFromIndex(newTargetindex, reaction);

        return new ChangeTarget(timeCommandExecuted, acceptanceRate, newTarget, commandId);
    }


    private LinkedList<Integer> getTargetFromIndex(int newTargetIndex, LinkedList<Integer> targets) {

        LinkedList<Integer> nextTarget = new LinkedList<>();
        int newTargetId = targets.get(newTargetIndex);
        nextTarget.add(newTargetId);
        return nextTarget;
    }

    private double getReactionProbabilityFromIndex(int newTargetIndex, LinkedList<Double> acceptanceRates) {

        if (acceptanceRates.size() == 1){
            return acceptanceRates.getFirst();
        }
        return acceptanceRates.get(newTargetIndex);
    }

    private int getIndexFromRandomDistribution(final LinkedList<Double> probabilityValues) {

        LinkedList<Integer> indices = (LinkedList<Integer>) IntStream.range(0, probabilityValues.size()).boxed().collect(Collectors.toList());
        EnumeratedIntegerDistribution dist = getDiscreteDistribution(indices, probabilityValues);
        return dist.sample();
    }

    private EnumeratedIntegerDistribution getDiscreteDistribution(LinkedList<Integer> possibleTargets, LinkedList<Double> probs){

        RandomGenerator rng = new JDKRandomGenerator(random.nextInt());
        return new EnumeratedIntegerDistribution(rng,
                possibleTargets.stream().mapToInt(i -> i).toArray(),
                probs.stream().mapToDouble(i -> i).toArray());
    }

    // read required target distribution from json, read acceptance rate from json

    private LinkedList<Integer> readTargetsFromJson(JSONObject command) {
        LinkedList<Integer> targets = new LinkedList<>();
        JSONArray targetList = (JSONArray) command.get("targetIds");

        for (Object t : targetList) {
            targets.add((Integer) t);
        }
        return targets;
    }

    private LinkedList<Double> readProbabilitiesFromJson(JSONObject command) {
        LinkedList<Double> probs = new LinkedList<>();
        JSONArray targetList = (JSONArray) command.get("probability");

        for (int i = 0; i < targetList.length(); i++) {
            probs.add(targetList.getDouble(i));
        }
        return probs;
    }

    private LinkedList<Double> readReactionProbabilitiesFromJson(JSONObject command) {
        LinkedList<Double> probs = new LinkedList<>();

        JSONArray targetList = (JSONArray) command.get("reactionProbability");

        for (int i = 0; i < targetList.length(); i++) {
            probs.add(targetList.getDouble(i));
        }
        return probs;
    }


}
