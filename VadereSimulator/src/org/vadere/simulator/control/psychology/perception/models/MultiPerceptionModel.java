package org.vadere.simulator.control.psychology.perception.models;

import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provide stimuli for cognition model
 */
public class MultiPerceptionModel extends PerceptionModel {

    private Topography topography;


    @Override
    public void initialize(Topography topography, final double simTimeStepLengh) {
        this.topography = topography;
    }

    @Override
    public void update(HashMap<Pedestrian, List<Stimulus>> pedSpecificStimuli) {

        for (Map.Entry<Pedestrian, List<Stimulus>> pedStimuli : pedSpecificStimuli.entrySet()) {

            LinkedList<Stimulus> stimuli = pedStimuli.getValue().stream().collect(Collectors.toCollection(LinkedList::new));
            LinkedList<Stimulus> stimuli2 = new LinkedList<>();
            stimuli2.addAll(stimuli);

            Pedestrian ped = pedStimuli.getKey();

            for (Stimulus stimulus : stimuli2){
                if (stimulus instanceof RouteRecommendation){
                    LinkedList<Stimulus> stimulusChangeTarget = ((RouteRecommendation) stimulus).unpackChangeTargetStimuli();
                    stimuli.remove(stimulus);
                    stimuli.addAll(stimulusChangeTarget);
                }
            }

            ped.setNextPerceivedStimuli(stimuli);
        }

    }


}
