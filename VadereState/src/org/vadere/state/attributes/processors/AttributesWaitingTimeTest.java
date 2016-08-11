package org.vadere.state.attributes.processors;

import org.vadere.state.attributes.Attributes;

/**
 * Attributes tested by the PedestrianWaitingTimeTest.
 * <br>
 * maxWaitingTime: maximum waiting time each pedestrian is allowed to wait.<br>
 * maxWaitingTimeMean: maximum of mean waiting time the pedestrians are allowed to wait.<br>
 * minWaitingTime: minimum waiting time each pedestrian is allowed to wait.<br>
 * minWaitingTimeMean: minimum of mean waiting time the pedestrians are allowed to wait.<br>
 * expectFailure: true if the test should fail with the current setup.
 * 
 *
 */
public class AttributesWaitingTimeTest extends Attributes {
	private double maxWaitingTime = 10.0;
	private double maxWaitingTimeMean = 5.0;
	private double minWaitingTime = 0.0;
	private double minWaitingTimeMean = 0.0;
	private boolean expectFailure = false;

	public double getMaxWaitingTime() {
		return maxWaitingTime;
	}

	public double getMaxWaitingTimeMean() {
		return maxWaitingTimeMean;
	}

	public double getMinWaitingTime() {
		return minWaitingTime;
	}

	public double getMinWaitingTimeMean() {
		return minWaitingTimeMean;
	}

	public boolean isExpectFailure() {
		return expectFailure;
	}
}
