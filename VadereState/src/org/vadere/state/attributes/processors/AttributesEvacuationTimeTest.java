package org.vadere.state.attributes.processors;

import org.vadere.state.attributes.Attributes;

/**
 * Attributes tested by the PedestrianEvacuationTimeTest.
 * <br>
 * maxEvacuationTime: maximum waiting time each pedestrian is allowed to evacuate.<br>
 * maxEvacuationTimeMean: maximum of mean waiting time the pedestrians are allowed to evacuate.<br>
 * minEvacuationTime: minimum waiting time each pedestrian is allowed to evacuate.<br>
 * minEvacuationTimeMean: minimum of mean waiting time the pedestrians are allowed to evacuate.<br>
 * expectFailure: true if the test should fail with the current setup.
 * 
 *
 */
public class AttributesEvacuationTimeTest extends Attributes {
	private double maxEvacuationTime = 10.0;
	private double maxEvacuationTimeMean = 5.0;
	private double minEvacuationTime = 0.0;
	private double minEvacuationTimeMean = 0.0;
	private boolean expectFailure = false;

	public double getMaxEvacuationTime() {
		return maxEvacuationTime;
	}

	public double getMaxEvacuationTimeMean() {
		return maxEvacuationTimeMean;
	}

	public double getMinEvacuationTime() {
		return minEvacuationTime;
	}

	public double getMinEvacuationTimeMean() {
		return minEvacuationTimeMean;
	}

	public boolean isExpectFailure() {
		return expectFailure;
	}
}
