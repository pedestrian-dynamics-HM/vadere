package org.vadere.state.events;

import org.vadere.state.scenario.ScenarioElement;

import java.util.ArrayList;
import java.util.List;

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
 * concrete implementations. For instance, an event "ElapsedTimeEvent" can provide
 * the current time step. The event "Bang" can have an intensity and a polygon
 * which describes where the bang can be perceived.
 *
 * TODO Add following events:
 * - TimeElapsed
 * - Wait
 * - Bang
 */
public abstract class Event {

    protected double time;
    protected List<ScenarioElement> targets;

    protected Event(double time) {
        this.time = time;
        this.targets = new ArrayList<ScenarioElement>();
    }

    protected Event(double time, List<ScenarioElement> targets) {
        this.time = time;
        this.targets = targets;
    }

    // TODO Implement equals(), hashCode() and toString().
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

}
