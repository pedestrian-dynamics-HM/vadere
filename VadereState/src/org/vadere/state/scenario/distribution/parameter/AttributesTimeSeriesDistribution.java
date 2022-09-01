package org.vadere.state.scenario.distribution.parameter;

import org.vadere.state.attributes.distributions.AttributesDistribution;

/**
 * @author Lukas Gradl (lgradl@hm.edu)
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
