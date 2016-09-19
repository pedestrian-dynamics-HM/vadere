package org.vadere.state.simulation;

import java.util.Optional;

/**
 * Java Bean that store the stepNumber and the simulation time in seconds of a specific time step.
 *
 */
public class Step implements Comparable<Step> {
	private final Integer stepNumber;
	private final Double simTimeInSec;

	public Step(final int stepNumber) {
		this.stepNumber = stepNumber;
		this.simTimeInSec = null;
	}

	/**
	 * Returns an Optional<Double> since the simulation time in seconds may not stored.
	 * 
	 * @return an Optional<Double>
	 */
	public Optional<Double> getSimTimeInSec() {
		if (simTimeInSec == null) {
			return Optional.empty();
		}
		return Optional.of(simTimeInSec);
	}

	/**
	 * The number of this step. The smallest step number is 1.
	 * 
	 * @return number of this step
	 */
	public int getStepNumber() {
		return stepNumber;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		Step other = (Step) obj;
		return stepNumber.equals(other.stepNumber);
	}

	@Override
	public String toString() {
		return "(" + stepNumber + ", " + simTimeInSec + ")";
	}

	@Override
	public int hashCode() {
		return 31 * stepNumber;
	}

	@Override
	public int compareTo(final Step o) {
		return this.getStepNumber() - o.getStepNumber();
	}
}
