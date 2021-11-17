package org.vadere.simulator.control.external.models;

import org.apache.commons.math3.util.Precision;
import org.json.JSONObject;
import org.junit.Test;
import org.vadere.state.psychology.perception.types.DistanceRecommendation;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.util.io.IOUtils;

import java.io.IOException;

public class SocialDistancingTest {


    private String readInputFile(){
        String dataPath = "testResources/control/external/SocialDistancingData.json";
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
        JSONObject command = json.getJSONObject("command");
        int commandId = 33;
        double timeCommandExecuted = 5.0;
        double socialDistance = 1.33;
        double cloggingTime = 0.0;

        SocialDistancing sd = new SocialDistancing();
        Stimulus stimulus = sd.getStimulusFromJsonCommand( command, commandId, timeCommandExecuted);
        assert (stimulus instanceof DistanceRecommendation);
        assert Precision.equals(((DistanceRecommendation) stimulus).getSocialDistance(), socialDistance, 0.001);
        assert Precision.equals(((DistanceRecommendation) stimulus).getCloggingTimeAllowedInSecs(), cloggingTime, 0.001);

    }




}
