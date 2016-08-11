package org.vadere.simulator.projects.dataprocessing;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * One {@link TimeStep} object contains a List&lt;KinematicData&gt;, a time
 * (double) and a step (int, number of steps until here).
 * 
 */
public class TimeStep {
	public final List<TimeStepData> data;
	public final double time;
	public final int step;

	/**
	 * A new {@link TimeStep} object.
	 * 
	 * @param kinematics
	 *        kinematic data for this time step.
	 * @param time
	 *        initial point in time.
	 * @param step
	 *        step number of this time step.
	 */
	public TimeStep(Collection<? extends TimeStepData> kinematics, double time, int step) {
		if (kinematics != null) {
			this.data = new LinkedList<TimeStepData>(kinematics);
		} else {
			this.data = null;
		}

		this.time = time;
		this.step = step;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TimeStep other = (TimeStep) obj;
		if (data == null) {
			if (other.data != null) {
				return false;
			}
		} else if (!data.equals(other.data)) {
			return false;
		}
		if (step != other.step) {
			return false;
		}
		if (Double.doubleToLongBits(time) != Double
				.doubleToLongBits(other.time)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + step;
		long temp;
		temp = Double.doubleToLongBits(time);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
}
