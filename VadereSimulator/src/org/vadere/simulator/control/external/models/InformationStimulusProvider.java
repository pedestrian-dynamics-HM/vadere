package org.vadere.simulator.control.external.models;

import org.json.JSONObject;
import org.vadere.state.psychology.perception.types.DistanceRecommendation;
import org.vadere.state.psychology.perception.types.InformationStimulus;
import org.vadere.state.psychology.perception.types.Stimulus;

import java.util.Random;


public class InformationStimulusProvider extends ControlModel {


    public InformationStimulusProvider() {
        super();
    }


    @Override
    protected Stimulus getStimulusFromJsonCommand(JSONObject command, int stimulusId, double timeCommandExecuted) {
        String instruction = readSocialDistanceFromJson(command);
        return new InformationStimulus(instruction);
    }



    private String readSocialDistanceFromJson(JSONObject command) {
        return command.getString("instruction");
    }


}
