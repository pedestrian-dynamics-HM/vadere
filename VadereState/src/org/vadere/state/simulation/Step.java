package org.vadere.state.simulation;


/**
 * Immutable class. Java Bean that store the stepNumber and the simulation time in seconds of a specific time step.
 * A simulation step is defined by <tt>simTimeStepLength</tt> that is if t is the simulation time than
 * ceil(t / <tt>simTimeStepLength</tt>) is the simulation {@link Step}.
 *
 * @author Benedikt Zoennchen
 *
 */
public class Step implements Comparable<Step> {
	private final int stepNumber;
	private final static int MINIMAL_STEP = 1;
	private final static Step FIST_STEP = new Step(1);
	private final static double MIN_TOLERANCE = 0.001;
	private final static double MAX_TOLERANCE = 0.999;

	public Step(final int stepNumber) {
		this.stepNumber = stepNumber;
	}

	/**
	 * The number of this step. The smallest step number is 1.
	 * 
	 * @return number of this step
	 */
	public int getStepNumber() {
		return stepNumber;
	}

	/**
	 * Securely increments the step.
	 *
	 * @return the incremented step
	 */
	public Step increment() {
		return new Step(stepNumber + 1);
	}

	/**
	 * Securely substracts the <tt>step</tt> from <tt>this</tt>.
	 *
	 * @param step a step
	 * @return the subtraction result or the minimal step if the result would generate a step smaller than the minimal step.
	 */
	public Step subtract(final Step step) {
		int diff = stepNumber - step.getStepNumber();

		if (diff >= MINIMAL_STEP) {
			return new Step(diff);
		} else {
			return FIST_STEP;
		}
	}

	/**
	 * Securely decrements the step by 1.
	 *
	 * @return the decremented step or this step (if this step is the minimal step).
	 */
	public Step decrement() {
		return new Step(stepNumber - 1);
	}

	public boolean isGreaterThan(final Step step) {
		return compareTo(step) > 0;
	}

	public boolean isGreaterEqThan(final Step step) {
		return compareTo(step) >= 0;
	}

	public boolean isSmallerThan(final Step step) {
		return compareTo(step) < 0;
	}

	public boolean isSmallerEqThan(final Step step) {
		return compareTo(step) <= 0;
	}

	public static int toFloorStep(final double simTimeInSec, final double simStepLengthInSec) {
		Step base = new Step((int) (simTimeInSec / simStepLengthInSec));
		double r = simTimeInSec - toSimTimeInSec(base.getStepNumber(), simStepLengthInSec);

		if(r / simStepLengthInSec > MAX_TOLERANCE) {
			return base.increment().getStepNumber();
		} else{
			return base.getStepNumber();
		}
	}

	public static int toCeilStep(final double simTimeInSec, final double simStepLengthInSec) {
		Step base = new Step((int) (simTimeInSec / simStepLengthInSec));
		double r = simTimeInSec - toSimTimeInSec(base.getStepNumber(), simStepLengthInSec);

		if(r / simStepLengthInSec < MIN_TOLERANCE) {
			return base.getStepNumber();
		} else {
			return base.increment().getStepNumber();
		}
	}

	public static double toSimTimeInSec(final int step, final double simStepLengthInSec) {
		return step * simStepLengthInSec;
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
		return stepNumber == other.stepNumber;
	}

	@Override
	public String toString() {
		return "(" + stepNumber + ")";
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
