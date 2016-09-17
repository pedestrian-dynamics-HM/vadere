package org.vadere.state.attributes.models.seating;

import org.apache.commons.math3.util.Pair;

public class ValueWithProbabilityFraction<T> {
	
	private T value;
	private double fraction;

	public ValueWithProbabilityFraction() { }

	public ValueWithProbabilityFraction(T value, double fraction) {
		this.value = value;
		this.fraction = fraction;
	}

	public T getValue() {
		return value;
	}

	public double getFraction() {
		return fraction;
	}
	
	public Pair<T, Double> toPair() {
		return new Pair<>(value, fraction);
	}
	
}
