package org.vadere.simulator.control.cognition;

import org.vadere.state.psychology.stimuli.types.*;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The EventCognition class should provide logic to prioritize {@link Stimulus}s for a pedestrian based on its
 * current state/attributes (e.g., a {@link Bang} is more important than a {@link Wait}.
 */
public class EventCognition {

    public void prioritizeEventsForPedestrians(List<Stimulus> stimuli, Collection<Pedestrian> pedestrians){
        for (Pedestrian pedestrian : pedestrians) {
            // TODO: prioritize the stimuli for the current time step for each pedestrian individually.
            //   by using a finite state machine, weight pedestrian's attributes or any other good mechanism.
            Stimulus mostImportantStimulus = rankWaitHigherThanElapsedTime(stimuli, pedestrian);
            pedestrian.setMostImportantStimulus(mostImportantStimulus);
        }
    }

    private Stimulus rankWaitHigherThanElapsedTime(List<Stimulus> stimuli, Pedestrian pedestrian) {
        // TODO: replace dummy implementation here.
        Stimulus mostImportantStimulus = stimuli.stream()
                .filter(event -> event instanceof ElapsedTime)
                .collect(Collectors.toList())
                .get(0);

        List<Stimulus> waitStimuli = stimuli.stream().filter(event -> event instanceof Wait).collect(Collectors.toList());
        List<Stimulus> waitInAreaStimuli = stimuli.stream().filter(event -> event instanceof WaitInArea).collect(Collectors.toList());
        List<Stimulus> bangStimuli = stimuli.stream().filter(event -> event instanceof Bang).collect(Collectors.toList());
        List<Stimulus> changeTargetStimuli = stimuli.stream().filter(event -> event instanceof ChangeTarget).collect(Collectors.toList());

        if (changeTargetStimuli.size() >= 1) {
            mostImportantStimulus = changeTargetStimuli.get(0);
        } else if (bangStimuli.size() >= 1) {
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
