package org.vadere.simulator.control.psychology.perception.models;

import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.*;

/**
 * Use a very simple strategy to rank stimulus priority:
 *
 * ChangeTargetScripted > ChangeTarget > Threat > Wait > WaitInArea > ElapsedTime
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

            List<Stimulus> stimuli = pedStimuli.getValue();
            Pedestrian ped = pedStimuli.getKey();
            ped.setNextPerceivedStimuli((LinkedList<Stimulus>) stimuli);
        }

    }

}
