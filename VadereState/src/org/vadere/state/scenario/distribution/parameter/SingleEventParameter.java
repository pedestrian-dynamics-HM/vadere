package org.vadere.state.scenario.distribution.parameter;


/**
 * This is the parameter structure used with a single spawn distribution.
 * @author Lukas Gradl (lgradl@hm.edu), Ludwig Jaeck
 */

public class SingleEventParameter {
	public double getEventTime() {
		return eventTime;
	}

	public void setEventTime(double eventTime) {
		this.eventTime = eventTime;
	}

	/**
	 * The attribute spawnTime describes the time the event occurs.
	 */
	double eventTime;

	public int getSpawnNumber() {
		return spawnNumber;
	}

	public void setSpawnNumber(int spawnNumber) {
		this.spawnNumber = spawnNumber;
	}

	int spawnNumber;


}
