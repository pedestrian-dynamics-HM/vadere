package org.vadere.state.psychology.perception.json;

import org.vadere.state.psychology.perception.types.Location;
import org.vadere.state.psychology.perception.types.Stimulus;
import org.vadere.state.psychology.perception.types.SubpopulationFilter;
import org.vadere.state.psychology.perception.types.Timeframe;

import java.util.LinkedList;
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
    private Location location;
    private SubpopulationFilter subpopulationFilter;
    private List<Stimulus> stimuli;

    public StimulusInfo() {
        this(new Timeframe(), new LinkedList<>(), new Location(), new SubpopulationFilter());
    }

    public StimulusInfo(Timeframe timeframe, List<Stimulus> stimuli, Location location, SubpopulationFilter subpopulationFilter) {
        this.timeframe = timeframe;
        this.stimuli = stimuli;
        this.location = location;
        this.subpopulationFilter = subpopulationFilter;
    }

    // Getter
    public Timeframe getTimeframe() {
        return timeframe;
    }
    public List<Stimulus> getStimuli() {
        return stimuli;
    }
    public Location getLocation() {return location;}
    public SubpopulationFilter getSubpopulationFilter() {return subpopulationFilter;}



    // Setter
    public void setTimeframe(Timeframe timeframe) {
        this.timeframe = timeframe;
    }
    public void setStimuli(List<Stimulus> stimuli) {
        this.stimuli = stimuli;
    }
    public void setLocation(Location location) {this.location = location;}
    public void setSubpopulationFilter(SubpopulationFilter subpopulationFilter) {this.subpopulationFilter = subpopulationFilter;}



}
