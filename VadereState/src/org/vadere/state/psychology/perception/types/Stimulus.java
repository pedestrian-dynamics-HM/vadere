package org.vadere.state.psychology.perception.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

/**
 * The base class of all available stimuli.
 *
 * A stimulus has a time and possibly additional information.
 *
 * The additional information depend on the type of the stimulus and should be
 * added by subclasses. For instance, a stimulus "ElapsedTime" can provide
 * the current time. A stimulus "Threat" can have a loudness and a polygon
 * which describes where the threat can be perceived.
 *
 * This class and its subclasses should be de-/serialized as JSON. Therefore,
 * provide some annotations so that serialized objects do not reveal Java
 * type information like "util.ArrayList".
 *
 * See @link http://www.baeldung.com/jackson-inheritance
 *
 * Watch out: subclasses require a default constructor so that
 * de-/serialization works!
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @Type(value = Threat.class, name = "Threat"),
        @Type(value = ElapsedTime.class, name = "ElapsedTime"),
        @Type(value = Wait.class, name = "Wait"),
        @Type(value = WaitInArea.class, name = "WaitInArea"),
        @Type(value = ChangeTarget.class, name = "ChangeTarget"),
        @Type(value = ChangeTargetScripted.class, name = "ChangeTargetScripted")
})
// "time" is set when the stimulus is injected into the simulation run and must not be de-/serialized.
@JsonIgnoreProperties({ "time" })
public abstract class Stimulus implements Cloneable {

    // Member Variables
    protected double time;

    // Constructors
    // Default constructor required for JSON de-/serialization.
    protected Stimulus() {
        this.time = 0;
    }

    protected Stimulus(double time) {
        this.time = time;
    }

    protected Stimulus(Stimulus other) {
        this(other.time);
    }

    // Getter
    public double getTime() {
        return time;
    }

    // Setter
    public void setTime(double time) {
        this.time = time;
    }

    // Methods
    @Override
    public abstract Stimulus clone();

    // Static Methods
    public static boolean listContainsStimulus(List<Stimulus> stimuli, Class<? extends Stimulus> eventToCheck) {
        return stimuli.stream().anyMatch(event -> event.getClass().equals(eventToCheck));
    }

    @Override
    public String toString() {
        String string = String.format("%s:\n", this.getClass().getSimpleName());
        string += String.format("  time: %f\n", time);
        return string;
    }

    public String toStringForOutputProcessor() {
        return this.getClass().getSimpleName();
    }

}
