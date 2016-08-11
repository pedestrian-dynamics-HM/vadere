package org.vadere.state.attributes.processors;

import org.vadere.state.attributes.Attributes;

public class AttributesWriter extends Attributes {

	private double startTime = 0.0;

	private double endTime = Double.MAX_VALUE;

	public double getStartTime() {
		return startTime;
	}

	public double getEndTime() {
		return endTime;
	}
}
