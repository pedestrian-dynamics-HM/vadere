package org.vadere.simulator.control.external.models;


import org.json.JSONObject;
import org.vadere.simulator.control.external.reaction.InformationFilterSettings;
import org.vadere.simulator.control.psychology.perception.StimulusController;
import org.vadere.state.psychology.information.InformationState;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.logging.Logger;
import rx.Subscription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public abstract class ControlModel implements IControlModel, InformationModel {

    public static Logger logger = Logger.getLogger(Subscription.class);

    public Topography topography;
    protected StimulusController stimulusController;
    private double simTimeStepLength;
    protected InformationFilter informationFilter;

    public ControlModel(){

    }

    @Override
    public void init(final Topography topography, final StimulusController stimulusController, final double simTimeStepLength, final InformationFilterSettings informationFilterSettings) {
        this.topography = topography;
        this.stimulusController = stimulusController;
        this.simTimeStepLength = simTimeStepLength;
        this.informationFilter = new InformationFilter(informationFilterSettings);
    }

    protected abstract void generateStimulusforPed(Pedestrian ped, JSONObject command, int commandId, double timeCommandExecuted);

    public void update(String commandRaw, Double time, int pedId)  {

        CtlCommand command = new CtlCommand(commandRaw);

        Collection<Pedestrian> pedestrians;
        if (pedId == -1){
            pedestrians = new ArrayList<>(topography.getPedestrianDynamicElements().getElements());
        }
        else{
            pedestrians = topography.getPedestrianDynamicElements().getElements().stream().filter(pedestrian -> pedestrian.getId() == pedId).collect(Collectors.toList());
        }

        for (Pedestrian ped : pedestrians) {
            if (this.informationFilter.isInformationProcessed(ped, command.getSpace(), time, command.getExecTime(), command.getCommandId())){
                this.generateStimulusforPed(ped, command.getPedCommand(), command.getCommandId(), getTimeCommandExecuted(time));

                this.informationFilter.setPedProcessedCommandIds(ped, command.getCommandId());
                setInformationState(ped);

                for (Pedestrian groupMember : ped.getPedGroupMembers()){
                    this.informationFilter.setPedProcessedCommandIds(ped, command.getCommandId());
                    setInformationStateGroupMember(groupMember);
                }
            }



        }


    }



    public double getTimeCommandExecuted(double currentTime) {
        return currentTime + simTimeStepLength;
    }



}
