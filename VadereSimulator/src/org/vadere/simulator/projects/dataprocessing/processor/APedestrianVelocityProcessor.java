package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;

public abstract class APedestrianVelocityProcessor extends DataProcessor<TimestepPedestrianIdKey, Double> {
	public APedestrianVelocityProcessor() {
		super("velocity");
	}
}
