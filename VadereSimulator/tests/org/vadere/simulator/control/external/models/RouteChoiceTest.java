package org.vadere.simulator.control.external.models;


import org.apache.commons.math3.util.Precision;
import org.json.JSONObject;
import org.junit.Test;
import org.vadere.simulator.control.external.reaction.InformationFilterSettings;
import org.vadere.simulator.control.psychology.perception.StimulusController;
import org.vadere.simulator.projects.ScenarioStore;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.ChangeTarget;
import org.vadere.state.psychology.perception.types.ElapsedTime;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


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

        JSONObject json;
        json = new JSONObject(readInputFile());

        JSONObject commmand = json.getJSONObject("command");

        int commandId = 1;
        double acceptanceRate = 0.72;
        double timeCommandExecuted = 1.3;
        LinkedList<Integer> targetIds = new LinkedList<>();
        targetIds.add(4);

        RouteChoice routeChoice = new RouteChoice();
        Stimulus stimulus = routeChoice.getStimulusFromJsonCommand(new Pedestrian(new AttributesAgent(), new Random(42)), commmand, commandId, timeCommandExecuted);

        ChangeTarget expectedStimulus = new ChangeTarget(timeCommandExecuted, acceptanceRate, targetIds, commandId);

        assertEquals(stimulus, expectedStimulus);





    }






}
