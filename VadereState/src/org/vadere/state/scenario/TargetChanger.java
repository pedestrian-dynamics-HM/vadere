package org.vadere.state.scenario;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesTargetChanger;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * An area with an arbitrary shape that changes the target of an agent.
 * Either to another static target or to another agent.
 */
public class TargetChanger extends ScenarioElement<AttributesTargetChanger> implements Comparable<TargetChanger> {

    // Member Variables
    private final Map<Integer, Double> enteringTimes;
    /**
     * Collection of listeners - unordered because it's order is not predictable
     * (at least not for clients).
     */
    private final Collection<TargetChangerListener> targetChangerListeners = new LinkedList<>();

    // Constructors
    public TargetChanger(AttributesTargetChanger attributes) {
        this(attributes, new HashMap<>());
    }

    public TargetChanger(AttributesTargetChanger attributes, Map<Integer, Double> enteringTimes) {
        this.attributes = attributes;
        this.enteringTimes = enteringTimes;
    }

    // Getters
    public Map<Integer, Double> getEnteringTimes() {
        return enteringTimes;
    }

    @Override
    public int getId() {
        return attributes.getId();
    }

    @Override
    public void setId(int id) {
        attributes.setId(id);
    }

    @Override
    public VShape getShape() {
        return attributes.getShape();
    }

    @Override
    public ScenarioElementType getType() {
        return ScenarioElementType.TARGET_CHANGER;
    }



    // Setters
    @Override
    public void setShape(VShape newShape) {
        attributes.setShape(newShape);
    }

    @Override
    public void setAttributes(AttributesTargetChanger attributes) {
        this.attributes =  attributes;
    }

    // Other Methods
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TargetChanger)) {
            return false;
        }
        TargetChanger other = (TargetChanger) obj;
        if (attributes == null) {
            if (other.attributes != null) {
                return false;
            }
        } else if (!attributes.equals(other.attributes)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(TargetChanger otherTarget) {
        return this.getId() - otherTarget.getId();
    }

    /** Models can register a target listener. */
    public void addListener(TargetChangerListener listener) {
        targetChangerListeners.add(listener);
    }

    public boolean removeListener(TargetChangerListener listener) {
        return targetChangerListeners.remove(listener);
    }

    /** Returns an unmodifiable collection. */
    public Collection<TargetChangerListener> getTargetChangerListeners() {
        return Collections.unmodifiableCollection(targetChangerListeners);
    }

    @Override
    public TargetChanger clone() {
        return new TargetChanger((AttributesTargetChanger) attributes.clone());
    }

}
