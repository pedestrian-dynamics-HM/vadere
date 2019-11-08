package org.vadere.state.psychology.perception.types;

/**
 * Class can signal agents to wait - for instance at a red traffic light.
 */
public class Wait extends Stimulus {

    // Default constructor required for JSON de-/serialization.
    public Wait() { super(); }

    public Wait(double time) {
        super(time);
    }

}
