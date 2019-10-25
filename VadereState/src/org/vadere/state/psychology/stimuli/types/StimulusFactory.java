package org.vadere.state.psychology.stimuli.types;

import org.jetbrains.annotations.NotNull;

/**
 * An event factory to convert strings into {@link Stimulus} objects.
 *
 * This is required to parse the output of output processors.
 */
public class StimulusFactory {

    public static Stimulus stringToStimulus(@NotNull String stimulusAsString) {
        Stimulus stimulusObject = null;

        if (stimulusAsString.matches(Bang.class.getSimpleName())) {
            stimulusObject = new Bang();
        } else if (stimulusAsString.matches(ElapsedTime.class.getSimpleName())) {
            stimulusObject = new ElapsedTime();
        } else if (stimulusAsString.matches(Wait.class.getSimpleName())) {
            stimulusObject = new Wait();
        } else if (stimulusAsString.matches(WaitInArea.class.getSimpleName())) {
            stimulusObject = new WaitInArea();
        }

        return stimulusObject;
    }
}
