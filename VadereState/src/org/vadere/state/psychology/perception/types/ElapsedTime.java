package org.vadere.state.psychology.perception.types;

/**
 * Class representing an elapsed time step.
 *
 * The class uses the inherited "time" to represent the current time step.
 */
public class ElapsedTime extends Stimulus {

    // Constructors
    // Default constructor required for JSON de-/serialization.
    public ElapsedTime() { super(); }

    public ElapsedTime(double time) {
        super(time);
    }

}
