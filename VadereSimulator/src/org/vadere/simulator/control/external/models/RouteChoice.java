package org.vadere.simulator.control.external.models;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vadere.state.psychology.perception.types.ChangeTarget;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.scenario.Pedestrian;

import java.util.LinkedList;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*
RouteChoice divides agents into different corridors. How the agents are to be distributed in percentage to the respective aisles is described with the help of a probability distribution.
For a distribution
[1,2,5] -> aisle name
[0.8,0.2,0.0] -> probability == desired distribution
80% of the people should take aisle 1 and 20% aisle 2.
In reality, the drawing of the random aisle could be done with the help of an app.
* */

public class RouteChoice extends ControlModel {

    private Random random;

    public RouteChoice() {
        super();
        // The seed for generating the random distribution is simply set to 0,
        // since the allocation should actually be deterministic (people should always be divided in the same way).
        random = new Random(0);
    }


    @Override
    protected Stimulus getStimulusFromJsonCommand(Pedestrian ped, JSONObject command, int stimulusId, double timeCommandExecuted) {

        LinkedList<Double> probs = readProbabilitiesFromJson(command);
        LinkedList<Integer> targets = readTargetsFromJson(command);

        int newTargetindex = getIndexFromRandomDistribution(probs);
        LinkedList<Integer> newTarget = getTargetFromIndex(newTargetindex, targets);

        return new ChangeTarget(timeCommandExecuted, newTarget, stimulusId);
    }


    private LinkedList<Integer> getTargetFromIndex(int newTargetIndex, LinkedList<Integer> targets) {

        LinkedList<Integer> nextTarget = new LinkedList<>();
        int newTargetId = targets.get(newTargetIndex);
        nextTarget.add(newTargetId);
        return nextTarget;
    }

    private int getIndexFromRandomDistribution(final LinkedList<Double> probabilityValues) {

        LinkedList<Integer> indices = IntStream.range(0, probabilityValues.size()).boxed().collect(Collectors.toCollection(LinkedList::new));
        EnumeratedIntegerDistribution dist = getDiscreteDistribution(indices, probabilityValues);
        return dist.sample();
    }


    private EnumeratedIntegerDistribution getDiscreteDistribution(LinkedList<Integer> possibleTargets, LinkedList<Double> probs){

        RandomGenerator rng = new JDKRandomGenerator(random.nextInt());
        return new EnumeratedIntegerDistribution(rng,
                possibleTargets.stream().mapToInt(i -> i).toArray(),
                probs.stream().mapToDouble(i -> i).toArray());
    }

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

}
