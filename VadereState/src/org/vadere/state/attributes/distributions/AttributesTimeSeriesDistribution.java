package org.vadere.state.attributes.distributions;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the parameter structure used with a time series distribution.
 * @author Lukas Gradl (lgradl@hm.edu), Ludwig Jaeck
 */

public class AttributesTimeSeriesDistribution extends AttributesDistribution {

	Double intervalLength = 0.0;
	List<Integer> spawnsPerInterval = new ArrayList<>();

	public double getIntervalLength() {
		return intervalLength;
	}

	public void setIntervalLength(double intervalLength) {
		this.intervalLength = intervalLength;
	}

	public List<Integer> getSpawnsPerInterval() {
		return spawnsPerInterval;
	}

	public void setSpawnsPerInterval(ArrayList<Integer> spawnsPerInterval) {
		this.spawnsPerInterval = spawnsPerInterval;
	}

}
