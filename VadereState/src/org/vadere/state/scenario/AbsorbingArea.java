package org.vadere.state.scenario;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAbsorbingArea;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * An area with an arbitrary shape that absorbs agents event if they have not reached their target.
 * This can be useful when agents run away from targets (e.g., after a threat stimulus).
 */
public class AbsorbingArea extends ScenarioElement<AttributesAbsorbingArea> implements Comparable<AbsorbingArea> {

	// Variables
	private final Map<Integer, Double> enteringTimes;
	/**
	 * Collection of listeners - unordered because it's order is not predictable
	 * (at least not for clients).
	 */
	private final Collection<AbsorbingAreaListener> absorbingAreaListeners = new LinkedList<>();

	// Constructors
	public AbsorbingArea(AttributesAbsorbingArea attributes) {
		this(attributes, new HashMap<>());
	}

	public AbsorbingArea(AttributesAbsorbingArea attributes, Map<Integer, Double> enteringTimes) {
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
		return ScenarioElementType.ABSORBING_AREA;
	}

	@Override
	public AttributesAbsorbingArea getAttributes() {
		return attributes;
	}

	// Setters
	@Override
	public void setShape(VShape newShape) {
		attributes.setShape(newShape);
	}

	@Override
	public void setAttributes(AttributesAbsorbingArea attributes) {
		this.attributes = attributes;
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
		if (!(obj instanceof AbsorbingArea)) {
			return false;
		}
		AbsorbingArea other = (AbsorbingArea) obj;
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
	public int compareTo(AbsorbingArea otherTarget) {
		return this.getId() - otherTarget.getId();
	}

	/** Models can register a target listener. */
	public void addListener(AbsorbingAreaListener listener) {
		absorbingAreaListeners.add(listener);
	}

	public boolean removeListener(AbsorbingAreaListener listener) {
		return absorbingAreaListeners.remove(listener);
	}

	/** Returns an unmodifiable collection. */
	public Collection<AbsorbingAreaListener> getAbsorbingAreaListeners() {
		return Collections.unmodifiableCollection(absorbingAreaListeners);
	}

	@Override
	public AbsorbingArea clone() {
		return new AbsorbingArea((AttributesAbsorbingArea) attributes.clone());
	}

}
