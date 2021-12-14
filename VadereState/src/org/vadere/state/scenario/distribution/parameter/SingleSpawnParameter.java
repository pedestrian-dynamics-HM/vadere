package org.vadere.state.scenario.distribution.parameter;


/**
 * @author Lukas Gradl (lgradl@hm.edu)
 */

public class SingleSpawnParameter {
	public double getSpawnTime() {
		return spawnTime;
	}

	public void setSpawnTime(double spawnTime) {
		this.spawnTime = spawnTime;
	}

	double spawnTime;

	public int getSpawnNumber() {
		return spawnNumber;
	}

	public void setSpawnNumber(int spawnNumber) {
		this.spawnNumber = spawnNumber;
	}

	int spawnNumber;


}
