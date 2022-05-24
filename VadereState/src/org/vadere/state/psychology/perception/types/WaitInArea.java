package org.vadere.state.psychology.perception.types;

import org.apache.commons.math3.util.Precision;
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

    public WaitInArea(double time, double probability) {
        super(time, probability);
    }
    public WaitInArea(double time, double probability, int id) {
        super(time, probability, id);
    }


    public WaitInArea(double time, VShape area) {
        super(time);
        this.area = area;
    }

    public WaitInArea(double time, VShape area, int id) {
        super(time, id);
        this.area = area;
    }

    public WaitInArea(WaitInArea other) {
        super(other.time);

        // According to BZ, "VShape" can be seen as immutable (i.e., usually
        // they have only "final" fields). Therefore, use "other.getArea()" directly
        // instead of cloning or allocating a new one.
        this.area = other.getArea();
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

    @Override
    public boolean equals(Object that){
        if(this == that) return true;
        if(!(that instanceof WaitInArea)) return false;
        WaitInArea wait = (WaitInArea) that;
        return this.area == wait.getArea();
    }

}
