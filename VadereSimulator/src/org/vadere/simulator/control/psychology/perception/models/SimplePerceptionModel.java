package org.vadere.simulator.control.psychology.perception.models;

import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use a very simple strategy to rank stimulus priority:
 *
 * ChangeTargetScripted > ChangeTarget > Threat > Wait > WaitInArea > ElapsedTime
 */
public class SimplePerceptionModel extends PerceptionModel {

    private Topography topography;

    @Override
    public void initialize(Topography topography, final double simTimeStepLengh) {
        this.topography = topography;
    }

    @Override
    public void update(HashMap<Pedestrian, List<Stimulus>> pedSpecificStimuli) {
        for (Pedestrian pedestrian : pedSpecificStimuli.keySet()) {
            Stimulus mostImportantStimulus = rankChangeTargetAndThreatHigherThanWait(pedSpecificStimuli.get(pedestrian), pedestrian);
            pedestrian.setMostImportantStimulus(mostImportantStimulus);
        }
    }


    private Stimulus rankChangeTargetAndThreatHigherThanWait(List<Stimulus> stimuli, Pedestrian pedestrian) {
        // Assume the "ElapsedTime" is the most important stimulus
        // unless there is something more important.
        Stimulus mostImportantStimulus = stimuli.stream()
                .filter(stimulus -> stimulus instanceof ElapsedTime)
                .collect(Collectors.toList())
                .get(0);

        List<Stimulus> waitStimuli = stimuli.stream().filter(stimulus -> stimulus instanceof Wait).collect(Collectors.toList());
        List<Stimulus> waitInAreaStimuli = stimuli.stream().filter(stimulus -> stimulus instanceof WaitInArea).collect(Collectors.toList());
        List<Stimulus> threatStimuli = stimuli.stream().filter(stimulus -> stimulus instanceof Threat).collect(Collectors.toList());
        List<Stimulus> changeTargetStimuli = stimuli.stream().filter(stimulus -> stimulus instanceof ChangeTarget).collect(Collectors.toList());
        List<Stimulus> changeTargetScriptedStimuli = stimuli.stream().filter(stimulus -> stimulus instanceof ChangeTargetScripted).collect(Collectors.toList());

        // place List changepersonalspace here

        if (changeTargetScriptedStimuli.size() >= 1) {
            mostImportantStimulus = changeTargetScriptedStimuli.get(0);
        } else if (changeTargetStimuli.size() >= 1) {
            mostImportantStimulus = changeTargetStimuli.get(0);
        } else if (threatStimuli.size() >= 1) {
            mostImportantStimulus = threatStimuli.get(0);
        } else if (waitStimuli.size() >= 1) {
            mostImportantStimulus = waitStimuli.get(0);
        } else if (waitInAreaStimuli.size() >= 1) {
            mostImportantStimulus = waitInAreaStimuli.get(0);

        }
        else if(true){} // place changepersonalspace here

        return mostImportantStimulus;
    }


}
