package org.vadere.state.attributes.distributions;

import org.vadere.state.attributes.distributions.AttributesDistribution;

/**
 * This is the parameter structure used with a time series distribution.
 * @author Lukas Gradl (lgradl@hm.edu), Ludwig Jaeck
 */

public class AttributesTimeSeriesDistribution extends AttributesDistribution {

	Double intervalLength;
	Integer[] spawnsPerInterval;

	public double getIntervalLength() {
		return intervalLength;
	}

	public void setIntervalLength(double intervalLength) {
		this.intervalLength = intervalLength;
	}

	public Integer[] getSpawnsPerInterval() {
		return spawnsPerInterval;
	}

	public void setSpawnsPerInterval(Integer[] spawnsPerInterval) {
		this.spawnsPerInterval = spawnsPerInterval;
	}

}
