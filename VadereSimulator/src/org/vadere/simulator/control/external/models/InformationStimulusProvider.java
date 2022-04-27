package org.vadere.simulator.control.external.models;

import org.json.JSONObject;
import org.vadere.state.psychology.perception.types.DistanceRecommendation;
import org.vadere.state.psychology.perception.types.InformationStimulus;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.scenario.Pedestrian;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.stream.Collectors;


public class InformationStimulusProvider extends ControlModel {


    public InformationStimulusProvider() {
        super();
    }


    @Override
    protected Stimulus getStimulusFromJsonCommand(JSONObject command, int stimulusId, double timeCommandExecuted) {
        String instruction = readSocialDistanceFromJson(command);
        return new InformationStimulus(timeCommandExecuted,instruction, stimulusId);
    }

    @Override
    public void update(String commandRaw, Double time, int pedId)  {

        CtlCommand command = new CtlCommand(commandRaw);

        Collection<Pedestrian> pedestrians;
        if (pedId == -1) pedestrians = new ArrayList<>(topography.getPedestrianDynamicElements().getElements());
        else pedestrians = topography.getPedestrianDynamicElements().getElements().stream().filter(pedestrian -> pedestrian.getId() == pedId).collect(Collectors.toList());

        for (Pedestrian ped : pedestrians) {

            if (this.informationFilter.isPedInDefinedArea(ped, command.getSpace())){
                Stimulus stimulus = this.getStimulusFromJsonCommand(command.getPedCommand(), command.getStimulusId(), getTimeCommandExecuted(time));
                setPedSpecificStimuli(time, ped, stimulus);
            } else {
                System.out.println(ped.getId() + " out of area.");
            }
        }
    }

    protected boolean isBehaviorChangeEnduring() {
        return false;
    }




    private String readSocialDistanceFromJson(JSONObject command) {
        return command.getString("instruction");
    }


}
