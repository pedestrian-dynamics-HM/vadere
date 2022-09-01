package org.vadere.state.scenario.distribution.parameter;



/**
 * This is the parameter structure used with a single spawn distribution.
 * @author Lukas Gradl (lgradl@hm.edu), Ludwig Jaeck
 */
public class SingleSpawnParameter {
	public double getSpawnTime() {
		return spawnTime;
	}

	public void setSpawnTime(double spawnTime) {
		this.spawnTime = spawnTime;
	}

	/**
	 * The attribute spawnTime describes the time the event occurs.
	 */
	double spawnTime;

	public int getSpawnNumber() {
		return spawnNumber;
	}

	public void setSpawnNumber(int spawnNumber) {
		this.spawnNumber = spawnNumber;
	}

	int spawnNumber;


}
