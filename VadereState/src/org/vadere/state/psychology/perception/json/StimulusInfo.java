package org.vadere.state.psychology.perception.json;

import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.psychology.perception.types.Timeframe;

import java.util.List;

/**
 * This class bundles one {@link Timeframe} and a list of {@link Stimulus} objects.
 * I.e., multiple stimuli can occur in a specified timeframe.
 *
 * This class is just a wrapper to get a convenient JSON de-/serialization.
 * The JSON serialization should look like this:
 *
 *      {
 *           "timeframe": {
 *               "startTime":...,
 *               "endTime":...,
 *               "repeat":...,
 *               "waitTimeBetweenRepetition":...
 *           },
 *           "stimuli": [
 *               { "type":"ElapsedTime" },
 *               {"type":"WaitInArea", "area": ... },
 *               ...
 *           ]
 *      }
 */
public class StimulusInfo {

    // Member Variables
    private Timeframe timeframe;
    private List<Stimulus> stimuli;

    // Getter
    public Timeframe getTimeframe() {
        return timeframe;
    }
    public List<Stimulus> getStimuli() {
        return stimuli;
    }

    // Setter
    public void setTimeframe(Timeframe timeframe) {
        this.timeframe = timeframe;
    }
    public void setStimuli(List<Stimulus> stimuli) {
        this.stimuli = stimuli;
    }

}
