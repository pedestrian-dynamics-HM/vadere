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
    HashMap<Pedestrian, List<Stimulus>> processedStimuli;


    @Override
    public void initialize(Topography topography, final double simTimeStepLengh) {
        this.topography = topography;
    }

    @Override
    public void update(HashMap<Pedestrian, List<Stimulus>> pedSpecificStimuli) {

        for (Map.Entry<Pedestrian, List<Stimulus>> pedStimuli : pedSpecificStimuli.entrySet()) {

            List<Stimulus> stimuli = pedStimuli.getValue();
            Pedestrian ped = pedStimuli.getKey();

            if (isStimuliRandomDistributionNew(stimuli, ped)) {
                ped.
            } else {
                updateStimulusTimes(stimuli, ped);
            }
            setInformationStateGroupMember(ped.getPedGroupMembers());
        }

    }


    private boolean isStimuliRandomDistributionNew(final List<Stimulus> stimuli, final Pedestrian pedestrian) {
        if (processedStimuli.containsKey(pedestrian)){
            List<Stimulus> oldStimuli = processedStimuli.get(pedestrian);
            return !oldStimuli.equals(stimuli);
        }
        return true;
    }

    protected void updateStimulusTimes(List<Stimulus> newStimuli, final Pedestrian pedestrian) {

        LinkedList<Stimulus> oldStimuli = pedestrian.getPerceivedStimuli();
        if (oldStimuli.equals(newStimuli)) {
            pedestrian.setPerceivedStimuli((LinkedList<Stimulus>) newStimuli);
        }
    }

    public void setProcessedStimuli(HashMap<Pedestrian, List<Stimulus>> processedStimuli) {
        this.processedStimuli = processedStimuli;
    }


}
