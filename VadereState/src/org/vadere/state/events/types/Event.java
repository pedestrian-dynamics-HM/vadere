package org.vadere.state.events.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.vadere.state.scenario.ScenarioElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The base class of all available events.
 *
 * An event has a time, targets and additional information.
 *
 * Targets are required so that an event can act on static scenario elements
 * like @see Obstacle or @see Target. For instance, then, you can define
 * an event which removes a @see Target from topography at a specific time
 * step.
 *
 * The additional information depend on the type of the event and are added by
 * subclasses. For instance, an event "ElapsedTimeEvent" can provide
 * the current time step. The event "Bang" can have an intensity and a polygon
 * which describes where the bang can be perceived.
 *
 * This class and its subclasses should be de-/serialized as JSON. Therefore,
 * provide some annotations so that serialized objects do not reveal Java
 * type information like "util.ArrayList".
 *
 * See @link http://www.baeldung.com/jackson-inheritance
 *
 * Watch out: subclasses require a default constructor so that
 * de-/serialization works.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @Type(value = ElapsedTimeEvent.class, name = "ElapsedTimeEvent"),
        @Type(value = WaitEvent.class, name = "WaitEvent"),
        @Type(value = WaitInAreaEvent.class, name = "WaitInAreaEvent")
})
// "time" is set when the event is actually raised and must not be de-/serialized.
@JsonIgnoreProperties({ "time" })
public abstract class Event {

    protected double time;
    protected List<ScenarioElement> targets;

    // Default constructor required for JSON de-/serialization.
    protected Event() {
        this.time = 0;
        this.targets = new ArrayList<ScenarioElement>();
    }

    protected Event(double time) {
        this.time = time;
        this.targets = new ArrayList<ScenarioElement>();
    }

    protected Event(double time, List<ScenarioElement> targets) {
        this.time = time;
        this.targets = targets;
    }

    public double getTime() {
        return time;
    }

    public List<ScenarioElement> getTargets() {
        return targets;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public void setTargets(List<ScenarioElement> targets) {
        this.targets = targets;
    }

    public static boolean listContainsEvent(List<Event> events, Class<? extends Event> eventToCheck) {
        return events.stream().anyMatch(event -> event.getClass().equals(eventToCheck));
    }

    @Override
    public String toString() {
        String targetsAsString = targets.stream().map(target -> target.getClass().getSimpleName()).collect(Collectors.joining(", "));

        String string = String.format("%s:\n", this.getClass().getSimpleName());
        string += String.format("  time: %f\n", time);
        string += String.format("  targets: [%s]\n", targetsAsString);

        return string;
    }

}
