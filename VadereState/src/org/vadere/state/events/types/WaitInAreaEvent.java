package org.vadere.state.events.types;

import org.vadere.state.scenario.ScenarioElement;
import org.vadere.util.geometry.shapes.VShape;

import java.util.List;

/**
 * This event hold as additional information: an area in which the event is valid.
 */
public class WaitInAreaEvent extends Event {

    private VShape area;

    // Default constructor required for JSON de-/serialization.
    public WaitInAreaEvent() { super(); }

    public WaitInAreaEvent(double time) {
        super(time);
    }

    public WaitInAreaEvent(double time, VShape area) {
        super(time);

        this.area = area;
    }

    public WaitInAreaEvent(double time, List<ScenarioElement> targets) {
        super(time, targets);
    }

    public VShape getArea() {
        return area;
    }

    public void setArea(VShape area) {
        this.area = area;
    }

}
