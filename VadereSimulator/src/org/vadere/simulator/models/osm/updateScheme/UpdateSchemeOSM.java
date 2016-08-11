package org.vadere.simulator.models.osm.updateScheme;


public interface UpdateSchemeOSM {

	public enum CallMethod {
		SEEK, MOVE, CONFLICTS, STEPS, RETRY, SEQUENTIAL, EVENT_DRIVEN
	}

	public void update(double timeStepInSec, double currentTimeInSec, CallMethod callMethod);
}
