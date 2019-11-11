package org.vadere.state.psychology.perception.types;

import org.vadere.util.geometry.shapes.VShape;

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

    public WaitInArea(WaitInArea other) {
        super(other.time);

        this.area = other.getArea().copy();
    }

    // Getter
    public VShape getArea() {
        return area;
    }

    // Setter
    public void setArea(VShape area) {
        this.area = area;
    }

    // Methods
    @Override
    public WaitInArea clone() {
        return new WaitInArea(this);
    }

}
