package org.vadere.simulator.control.psychology.perception.models;

import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use a very simple strategy to rank stimulus priority:
 *
 * ChangeTargetScripted > ChangeTarget > Threat > Wait > WaitInArea > ElapsedTime
 */
public class SimplePerceptionModel implements IPerceptionModel {

    private Topography topography;

    @Override
    public void initialize(Topography topography) {
        this.topography = topography;
    }

    @Override
    public void update(Collection<Pedestrian> pedestrians, List<Stimulus> stimuli) {
        for (Pedestrian pedestrian : pedestrians) {
            Stimulus mostImportantStimulus = rankChangeTargetAndThreatHigherThanWait(stimuli, pedestrian);
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
            Stimulus closestThreat = selectClosestAndPerceptibleThreat(pedestrian, threatStimuli);

            if (closestThreat != null) {
                mostImportantStimulus = closestThreat;
            }
        } else if (waitStimuli.size() >= 1) {
            mostImportantStimulus = waitStimuli.get(0);
        } else if (waitInAreaStimuli.size() >= 1) {
            Stimulus selectedWaitInArea = selectWaitInAreaContainingPedestrian(pedestrian, waitInAreaStimuli);

            if (selectedWaitInArea != null) {
                mostImportantStimulus = selectedWaitInArea;
            }
        }
        else if(true){} // place changepersonalspace here

        return mostImportantStimulus;
    }

    private Stimulus selectClosestAndPerceptibleThreat(Pedestrian pedestrian, List<Stimulus> threatStimuli) {
        Threat closestAndPerceptibleThreat = null;
        double distanceToClosestThreat = -1;

        for (Stimulus stimulus : threatStimuli) {
            Threat currentThreat = (Threat) stimulus;

            VPoint threatOrigin = topography.getTarget(currentThreat.getOriginAsTargetId()).getShape().getCentroid();
            double distanceToThreat = threatOrigin.distance(pedestrian.getPosition());

            if (distanceToThreat <= currentThreat.getRadius()) {
                if (closestAndPerceptibleThreat == null) {
                    closestAndPerceptibleThreat = currentThreat;
                    distanceToClosestThreat = distanceToThreat;
                } else {
                    if (distanceToThreat < distanceToClosestThreat) {
                        closestAndPerceptibleThreat = currentThreat;
                        distanceToClosestThreat = distanceToThreat;
                    }
                }
            }
        }

        return closestAndPerceptibleThreat;
    }

    private Stimulus selectWaitInAreaContainingPedestrian(Pedestrian pedestrian, List<Stimulus> waitInAreaStimuli) {
        WaitInArea selectedWaitInArea = null;

        for (Stimulus stimulus : waitInAreaStimuli) {
            WaitInArea waitInArea = (WaitInArea) stimulus;
            boolean pedInArea = waitInArea.getArea().contains(pedestrian.getPosition());

            if (pedInArea) {
                selectedWaitInArea = waitInArea;
            }
        }

        return selectedWaitInArea;
    }
}
