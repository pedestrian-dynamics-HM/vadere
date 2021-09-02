package org.vadere.simulator.control.external.models;


import org.apache.commons.math3.util.Precision;
import org.json.JSONObject;
import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.perception.types.ChangeTarget;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;


public class RouteChoiceTest {

    private String readInputFile(){
        String dataPath = "testResources/control/external/CorridorChoiceData.json";
        String msg = "";

        try {
            msg = IOUtils.readTextFile(dataPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return msg;
    }


    @Test
    public void generateStimulusFromCommand() {

        //The RouteChoice Model generates a stimulus distribution. The distribution is defined by the attribute probabilites.
        //Example
        //[11,12, 13] -> targetIds
        //[0.5,0.5,0.0] -> probabilites
        //means that 50% of the agents should go to target 11 and 50% to target 12.
        // No agent receives the instruction to go to target 13. Whether an agent reacts
        // to the instruction then depends on the psychological model chosen.

        JSONObject json;
        json = new JSONObject(readInputFile());
        JSONObject command = json.getJSONObject("command");

        int commandId = 1;
        double timeCommandExecuted = 1.3;
        LinkedList<Integer> targetIds = new LinkedList<>();
        targetIds.add(4);


        double[] shouldProbabilities = {0.25, 0, 0.25, 0.5}; //TODO read from json directly
        double[] isProbabilites = {0., 0., 0., 0.};
        int numberOfAgents = 10000;

        RouteChoice routeChoice = new RouteChoice();
        ChangeTarget stimulus = null;
        for (int i = 0; i < numberOfAgents; i++) {
            stimulus = (ChangeTarget) routeChoice.getStimulusFromJsonCommand(new Pedestrian(new AttributesAgent(), new Random(42)), command, commandId, timeCommandExecuted);
            int newTarget = stimulus.getNewTargetIds().getFirst();
            isProbabilites[newTarget] += 1./numberOfAgents; // targets =  {0, 1, 2, 3}; tagetsIds = indices
        }
        for (int ii = 0; ii < 4 ; ii++) Precision.equals(shouldProbabilities[ii], isProbabilites[ii], 0.02);

    }

    @Test
    public void generateStimulusFromEmptyCommand() {
        //TODO add

    }

    @Test
    public void generateStimulusFromOneOptionOnly() {
        //TODO add
    }






}
