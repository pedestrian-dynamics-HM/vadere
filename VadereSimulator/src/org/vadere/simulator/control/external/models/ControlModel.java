package org.vadere.simulator.control.external.models;


import org.json.JSONObject;
import org.vadere.simulator.control.external.reaction.ReactionModel;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.logging.Logger;
import rx.Subscription;

import java.util.LinkedList;
import java.util.stream.Collectors;

public abstract class ControlModel implements IControlModel {

    public static Logger logger = Logger.getLogger(Subscription.class);

    public Topography topography;
    public Double simTime;
    private CtlCommand command;
    protected ReactionModel reactionModel;

    public ControlModel(){
        simTime = 0.0;
        this.reactionModel = new ReactionModel();
    }

    public ControlModel(ReactionModel reactionModel){
        simTime = 0.0;
        this.reactionModel = reactionModel;
    }


    public abstract void applyPedControl(Pedestrian ped, JSONObject command);


    public void update(Topography topo, Double time, String commandStr, Integer pedId)  {
        topography = topo;
        simTime = time;
        command = new CtlCommand(commandStr);

        for (int i : get_pedIds(pedId)){
            Pedestrian ped = topography.getPedestrianDynamicElements().getElement(i);

            if (isInfoInTime() && isPedInDefinedArea(ped)){
                this.applyPedControl(ped, command.getPedCommand());
                this.setAction(ped);
            }

        }

    }

    public void update(Topography topography, Double time, String command) {
        update(topography,time,command,-1);

    }

    private LinkedList<Integer> get_pedIds(Integer pedId)
    {
        LinkedList<Integer> pedIds = new LinkedList<>();
        if (pedId == -1){
            pedIds.addAll(topography.getPedestrianDynamicElements().getElements().stream().map(Pedestrian::getId).collect(Collectors.toList()));
        }
        else{
            pedIds.add(pedId);
        }
        return  pedIds;
    }

    boolean isPedInDefinedArea(Pedestrian ped) {
        if (command.isSpaceBounded()) {
            return command.getSpace().contains(ped.getPosition());
        }
        else{
            return true;
        }
    }

    boolean isInfoInTime(){
        return command.getExecTime() >= simTime;
    }

    public abstract boolean isPedReact();

    public void setAction(Pedestrian ped){
        if (isPedReact()){
            triggerRedRaction(ped);
        }

    }

    protected abstract void triggerRedRaction(Pedestrian ped);

    public void setReactionModel(ReactionModel reactionModel){
        this.reactionModel = reactionModel;
    }


}
