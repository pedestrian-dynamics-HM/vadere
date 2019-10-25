package org.vadere.state.psychology.stimuli.types;

import org.vadere.state.scenario.ScenarioElement;
import org.vadere.util.geometry.shapes.VShape;

import java.util.List;

/**
 * This event hold as additional information: an area in which the event is valid.
 */
public class WaitInArea extends Stimulus {

    // Member Variables
    private VShape area;

    // Constructors
    // Default constructor required for JSON de-/serialization.
    public WaitInArea() { super(); }

    public WaitInArea(double time) {
        super(time);
    }

    public WaitInArea(double time, VShape area) {
        super(time);

        this.area = area;
    }

    // Getter
    public VShape getArea() {
        return area;
    }

    // Setter
    public void setArea(VShape area) {
        this.area = area;
    }

}
