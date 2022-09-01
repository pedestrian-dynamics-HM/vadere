package org.vadere.state.scenario.distribution.parameter;

/**
 * This is the parameter structure used with a time series distribution.
 * @author Lukas Gradl (lgradl@hm.edu), Ludwig Jaeck
 */
public class TimeSeriesParameter {

	double intervalLength;
	int[] spawnsPerInterval;

	public double getIntervalLength() {
		return intervalLength;
	}

	public void setIntervalLength(double intervalLength) {
		this.intervalLength = intervalLength;
	}

	public int[] getSpawnsPerInterval() {
		return spawnsPerInterval;
	}

	public void setSpawnsPerInterval(int[] spawnsPerInterval) {
		this.spawnsPerInterval = spawnsPerInterval;
	}

}
