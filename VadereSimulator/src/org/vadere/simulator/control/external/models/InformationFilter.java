package org.vadere.simulator.control.external.models;

import org.vadere.simulator.control.external.reaction.InformationFilterSettings;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import rx.Subscription;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * The {@link InformationFilter} The InformationFilter is used if information is only available to a limited extent.
 *
 * Possible restrictions are that
 ** Information is only available in a certain area (e.g. only within sight of a sign),
 ** information is only available at a certain time (dynamic light signal),
 ** the same information is only perceived once (recurring information is filtered by an app or human perception).
 ** only the first arriving information is perceived (new and contradictory information is filtered by an app or basically rejected by humans)
 */


public class InformationFilter {

    public static Logger logger = Logger.getLogger(Subscription.class);

    protected HashMap<Pedestrian, LinkedList<Integer>> processedCommandIds = new HashMap<>();
    protected boolean isReactingToFirstInformationOnly;
    protected boolean isReactingToRecurringInformation;


    public InformationFilter(InformationFilterSettings informationFilterSettings) {
        this.isReactingToFirstInformationOnly =  informationFilterSettings.isReactingToFirstInformationOnly();
        this.isReactingToRecurringInformation = informationFilterSettings.isReactingToRecurringInformation();
        this.processedCommandIds = new HashMap<>();
    }


    public InformationFilter(boolean isReactingToFirstInformationOnly, boolean isReactingToRecurringInformation){
        this.isReactingToFirstInformationOnly =  isReactingToFirstInformationOnly;
        this.isReactingToRecurringInformation = isReactingToRecurringInformation;
    }

    public InformationFilter(){
        this.isReactingToFirstInformationOnly = true;
        this.isReactingToRecurringInformation = false;
    }

    public boolean isInformationProcessed(Pedestrian ped, VShape shape, double simTime, double executionTime,  int commandId){
        return isInfoInTime(simTime, executionTime)
                && isPedInDefinedArea(ped, shape)
                && isProcessSameInfoAgain(ped, commandId)
                && isProcessFirstInformationOnly(ped);
    }

    public void setPedProcessedCommandIds(Pedestrian ped, int id){
        LinkedList<Integer> ids;

        if (this.processedCommandIds.containsKey(ped)) ids = this.processedCommandIds.get(ped);
        else ids = new LinkedList<>();

        if (!ids.contains(id)) ids.add(id);

        this.processedCommandIds.put(ped, ids);
    }

    boolean isProcessFirstInformationOnly(Pedestrian ped) {
        if (this.isReactingToFirstInformationOnly && processedCommandIds.containsKey(ped)){
            return processedCommandIds.get(ped).isEmpty();
        }
        return true;
    }


    boolean isProcessSameInfoAgain(Pedestrian ped, int id) {
        if (processedCommandIds.containsKey(ped)){
            if (processedCommandIds.get(ped).contains(id)){
                return this.isReactingToRecurringInformation;
            }
        }
        return true;
    }

    boolean isPedInDefinedArea(Pedestrian ped, VShape shape) {
        if (shape != null) {
            return shape.contains(ped.getPosition());
        }
        return true;
    }

    private boolean isInfoInTime(double simTime, double executionTime){
        return executionTime >= simTime;
    }






}
