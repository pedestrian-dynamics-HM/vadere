package org.vadere.simulator.control.psychology.perception.models;

import org.vadere.state.attributes.models.psychology.perception.AttributesMultiPerceptionModel;
import org.vadere.state.attributes.models.psychology.perception.AttributesPerceptionModel;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Use a very simple strategy to rank stimulus priority:
 *
 * ChangeTargetScripted > ChangeTarget > Threat > Wait > WaitInArea > ElapsedTime
 */
public class MultiPerceptionModel extends PerceptionModel {

    private Topography topography;
    private AttributesMultiPerceptionModel attributes;


    @Override
    public void initialize(Topography topography, final double simTimeStepLengh) {
        this.topography = topography;
        this.attributes = new AttributesMultiPerceptionModel();

    }

    @Override
    public void update(HashMap<Pedestrian, List<Stimulus>> pedSpecificStimuli) {

        for (Map.Entry<Pedestrian, List<Stimulus>> pedStimuli : pedSpecificStimuli.entrySet()) {

            LinkedList<Stimulus> stimuli = pedStimuli.getValue().stream().collect(Collectors.toCollection(LinkedList::new));
            Pedestrian ped = pedStimuli.getKey();
            ped.setNextPerceivedStimuli(stimuli);
        }

    }

    @Override
    public void setAttributes(AttributesPerceptionModel attributes) {
        this.attributes = (AttributesMultiPerceptionModel) attributes;

    }

    @Override
    public AttributesMultiPerceptionModel getAttributes() {
        return this.attributes;
    }


}
