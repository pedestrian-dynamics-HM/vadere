package org.vadere.state.scenario;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VShape;

import java.util.*;

public class Target extends ScenarioElement implements Comparable<Target> {

	private AttributesTarget attributes;
	private final Map<Integer, Double> enteringTimes;
	private final Map<Integer, Double> leavingTimes;
	
	/**
	 * Collection of listeners - unordered because it's order is not predictable
	 * (at least not for clients).
	 */
	private final Collection<TargetListener> targetListeners = new LinkedList<>();

	public enum WaitingBehaviour {
        INDIVIDUAL, TRAFFIC_LIGHT, NO_WAITING
	}
	public Target(AttributesTarget attributes) {
		this(attributes, new HashMap<>(),new HashMap<>());
	}

	public Target(AttributesTarget attributes, Map<Integer, Double> enteringTimes) {
		this(attributes,enteringTimes,new HashMap<>());
	}

	public Target(AttributesTarget attributes, Map<Integer, Double> enteringTimes, Map<Integer, Double> leavingTimes) {
		this.attributes = attributes;
		this.enteringTimes = enteringTimes;
		this.leavingTimes = leavingTimes;
	}


	public boolean isAbsorbing() {
		return attributes.isAbsorbing();
	}

	public int getParallelWaiters() {
		return attributes.getParallelEvents();
	}

	public Map<Integer, Double> getEnteringTimes() {
		return enteringTimes;
	}

	public Map<Integer, Double> getLeavingTimes(){
		return leavingTimes;
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
	public void setShape(VShape newShape) {
		attributes.setShape(newShape);
	}

	@Override
	public VShape getShape() {
		return attributes.getShape();
	}

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
		if (!(obj instanceof Target)) {
			return false;
		}
		Target other = (Target) obj;
		if (attributes == null) {
			return other.attributes == null;
		} else return attributes.equals(other.attributes);
	}

	@Override
	public ScenarioElementType getType() {
		return ScenarioElementType.TARGET;
	}

	@Override
	public AttributesTarget getAttributes() {
		return attributes;
	}

	@Override
	public void setAttributes(Attributes attributes) {
		this.attributes = (AttributesTarget) attributes;
	}



	/** Is this target actually a pedestrian? @see scenario.TargetPedestrian */
	public boolean isTargetPedestrian() {
		return false;
	}

	public boolean isMovingTarget() {
		return false;
	}

	@Override
	public int compareTo(Target otherTarget) {
		return this.getId() - otherTarget.getId();
	}

	/** Models can register a target listener. */
	public void addListener(TargetListener listener) {
		targetListeners.add(listener);
	}

	public boolean removeListener(TargetListener listener) {
		return targetListeners.remove(listener);
	}

	/** Returns an unmodifiable collection. */
	public Collection<TargetListener> getTargetListeners() {
		return Collections.unmodifiableCollection(targetListeners);
	}

	@Override
	public Target clone() {
		return new Target((AttributesTarget) attributes.clone());
	}

}
