package org.vadere.simulator.control.external.models;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vadere.simulator.control.external.reaction.ReactionModel;
import org.vadere.state.scenario.Pedestrian;

import java.util.LinkedList;
import java.util.Random;

public class RouteChoice extends ControlModel {

    private JSONObject command;
    private Random random;
    private LinkedList<Integer> newTargetList;


    public RouteChoice() {
        super();
        random = new Random(0);
        reactionModel = new ReactionModel();
    }


    public void applyPedControl(Pedestrian ped, JSONObject pedCommand) {

        command = pedCommand;
        // get information from controller
        newTargetList = getTargetFromNavigationApp();
    }

    @Override
    public boolean isPedReact() {

        LinkedList<Integer> alternativeTargets = readTargetsFromJson();
        int i = alternativeTargets.indexOf(newTargetList.get(0));

        try {
            return reactionModel.isPedReact(i);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;

    }

    @Override
    protected void triggerRedRaction(Pedestrian ped) {

        LinkedList<Integer> oldTarget = ped.getTargets();
        ped.setTargets(newTargetList);
        logger.debug("Pedestrian " + ped.getId() + ": changed target list from " + oldTarget + " to " + newTargetList);

    }


    private LinkedList<Integer> getTargetFromNavigationApp() {

        EnumeratedIntegerDistribution dist = getDiscreteDistribution(readTargetsFromJson(), readProbabilitiesFromJson());
        LinkedList<Integer> nextTarget = new LinkedList<>();

        nextTarget.add(dist.sample());
        return nextTarget;

    }

    private EnumeratedIntegerDistribution getDiscreteDistribution(LinkedList<Integer> possibleTargets, LinkedList<Double> probs){

        /**
         * Used for distributions from Apache Commons Math.
         */
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

        String jsonkey = "probability";

        JSONArray targetList = (JSONArray) command.get("probability");

        for (int i = 0; i < targetList.length(); i++) {
            probs.add(targetList.getDouble(i));
        }

        return probs;
    }





}
