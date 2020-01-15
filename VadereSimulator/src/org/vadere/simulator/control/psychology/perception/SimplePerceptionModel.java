package org.vadere.simulator.control.psychology.perception;

import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use a very simple strategy to rank stimulus priority:
 *
 * ChangeTarget > Bang > Wait > WaitInArea
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
            Stimulus mostImportantStimulus = rankChangeTargetAndBangHigherThanWait(stimuli, pedestrian);
            pedestrian.setMostImportantStimulus(mostImportantStimulus);
        }
    }

    private Stimulus rankChangeTargetAndBangHigherThanWait(List<Stimulus> stimuli, Pedestrian pedestrian) {
        // Assume the "ElapsedTime" is the most important stimulus
        // unless there is something more important.
        Stimulus mostImportantStimulus = stimuli.stream()
                .filter(stimulus -> stimulus instanceof ElapsedTime)
                .collect(Collectors.toList())
                .get(0);

        List<Stimulus> waitStimuli = stimuli.stream().filter(stimulus -> stimulus instanceof Wait).collect(Collectors.toList());
        List<Stimulus> waitInAreaStimuli = stimuli.stream().filter(stimulus -> stimulus instanceof WaitInArea).collect(Collectors.toList());
        List<Stimulus> bangStimuli = stimuli.stream().filter(stimulus -> stimulus instanceof Bang).collect(Collectors.toList());
        List<Stimulus> changeTargetStimuli = stimuli.stream().filter(stimulus -> stimulus instanceof ChangeTarget).collect(Collectors.toList());

        if (changeTargetStimuli.size() >= 1) {
            mostImportantStimulus = changeTargetStimuli.get(0);
        } else if (bangStimuli.size() >= 1) {
            // TODO: Select closest bang where pedestrian is within bang radius as most important stimulus.
            mostImportantStimulus = bangStimuli.get(0);
        } else if (waitStimuli.size() >= 1) {
            mostImportantStimulus = waitStimuli.get(0);
        } else if (waitInAreaStimuli.size() >= 1) {
            for (Stimulus stimulus : waitInAreaStimuli) {
                WaitInArea waitInArea = (WaitInArea) stimulus;

                boolean pedInArea = waitInArea.getArea().contains(pedestrian.getPosition());

                if (pedInArea) {
                    mostImportantStimulus = waitInArea;
                }
            }
        }

        return mostImportantStimulus;
    }
}
