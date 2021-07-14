package org.vadere.simulator.control.external.models;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vadere.simulator.control.external.reaction.ReactionModel;
import org.vadere.state.psychology.information.InformationState;
import org.vadere.state.psychology.perception.types.ChangeTarget;
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



    public void getControlAction(Pedestrian ped, JSONObject pedCommand) {

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
    protected void triggerPedReaction(Pedestrian ped) {

        LinkedList<Integer> oldTarget = ped.getTargets();

        if (isUsePsychologyLayer()) {
            // in this case the targets are set in the next time step when updating the psychology layer
            double timeCommandExecuted = getTimeOfNextStimulusAvailable();
            this.stimulusController.setDynamicStimulus(ped, new ChangeTarget(timeCommandExecuted, newTargetList), timeCommandExecuted);
            logger.debug("Pedestrian " + ped.getId() + ": created Stimulus ChangeTarget. New target list " + newTargetList);
        }else{
            ped.setTargets(newTargetList);
            ped.getKnowledgeBase().setInformationState(InformationState.INFORMATION_CONVINCING_RECEIVED);
            // in this case the targets are set directly
            logger.debug("Pedestrian " + ped.getId() + ": changed target list from " + oldTarget + " to " + newTargetList);
        }

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

    public boolean isInformationProcessed(Pedestrian ped, int commandId){

        // 1. handle conflicting instructions over time
        if (reactionModel.isReactingToFirstInformationOnly()){
            return isFirstInformation(ped);
        }

        // 2. handle recurring information that is received multiple times.

        // If a command is received, the naviation app checks
        // whether the command has already been displayed in an agent's app.

        // This is necessary when the information is disseminated through the mobile network
        // and can be received multiple times with different delays.
        // In this case, the information is not further processed.

        // Note: In the {@link ControlModel}, the agent makes the decision how to handle recurring information based on the reaction model setup.
        // Here, the navigation app decides on how to proceed recurring information (do not proceed it).

        return !isIdInList(ped, commandId);
    }

    public int getCommandId(){
        // The navigation app requires a unique command identifier.
        int id = super.getCommandId();
        if (id == 0){
            throw new IllegalArgumentException("Please provide a unique commandId != 0 for each command. Otherwise, information might not be processed.");
        }
        return id;
    }


}
