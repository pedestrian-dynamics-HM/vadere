package org.vadere.state.psychology.stimuli.exceptions;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.psychology.stimuli.types.ElapsedTime;
import org.vadere.state.psychology.stimuli.types.Stimulus;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;

/**
 * Use this exception if a stimuli-handling class does not support a specific stimulus.
 */
public class UnsupportedStimulusException extends RuntimeException {

    public UnsupportedStimulusException(@NotNull Stimulus unsupportedStimulus, @NotNull Class implementingClass) {
        super(String.format("Stimulus \"%s\" not supported by class \"%s\"!",
                unsupportedStimulus.getClass().getSimpleName(),
                implementingClass.getSimpleName())
        );
    }

    public static void throwIfNotElapsedTimeEvent(Collection<? extends Pedestrian> pedestrians, Class caller) {
        for (Pedestrian pedestrian : pedestrians) {
            Stimulus currentStimulus = pedestrian.getMostImportantStimulus();

            if ((currentStimulus instanceof ElapsedTime) == false) {
                throw new UnsupportedStimulusException(currentStimulus, caller);
            }
        }
    }
}
