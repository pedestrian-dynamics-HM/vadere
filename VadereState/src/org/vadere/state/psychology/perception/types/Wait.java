package org.vadere.state.psychology.perception.types;

import org.apache.commons.math3.util.Precision;

/**
 * Class can signal agents to wait - for instance at a red traffic light.
 */
public class Wait extends Stimulus {

    // Default constructor required for JSON de-/serialization.
    public Wait() { super(); }

    public Wait(double time) {
        super(time);
    }

    public Wait(double time, double probability) {
        super(time, probability);
    }

    public Wait(double time, double probability, int id) {
        super(time, probability, id);
    }


    public Wait(Wait other) { super(other.time); }

    // Methods
    @Override
    public Wait clone() {
        return new Wait(this);
    }

    @Override
    public boolean equals(Object that){
        if(this == that) return true;
        if(!(that instanceof Wait)) return false;
        Wait thatStimulus = (Wait) that;
        return true;
    }

}
