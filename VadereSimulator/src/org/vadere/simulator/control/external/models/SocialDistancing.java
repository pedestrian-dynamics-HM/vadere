package org.vadere.simulator.control.external.models;

import org.json.JSONObject;
import org.vadere.state.psychology.perception.types.DistanceRecommendation;
import org.vadere.state.psychology.perception.types.Stimulus;

import java.util.*;


public class SocialDistancing extends ControlModel {

    private Random random;

    public SocialDistancing() {
        super();
        // The seed for generating the random distribution is simply set to 0,
        // since the allocation should actually be deterministic (people should always be divided in the same way).
        random = new Random(0);
    }


    @Override
    protected Stimulus getStimulusFromJsonCommand(JSONObject command, int stimulusId, double timeCommandExecuted) {
        //TODO: ped here not necessary remove
        double distance = readSocialDistanceFromJson(command);
        double timeClogging = readTimeBeforeIgnoringDistancingFromJson(command);
        return new DistanceRecommendation(timeCommandExecuted, distance, timeClogging);
    }



    private double readSocialDistanceFromJson(JSONObject command) {
        return command.getDouble("socialDistance");
    }

    private double readTimeBeforeIgnoringDistancingFromJson(JSONObject command) {
        return command.getDouble("cloggingTimeAllowedInSecs");
    }

}
