package org.vadere.state.attributes.distributions;


/**
 * This is the parameter structure used with a single spawn distribution.
 * @author Lukas Gradl (lgradl@hm.edu), Ludwig Jaeck
 */

public class AttributesSingleSpawnDistribution extends AttributesDistribution {
	public double getSpawnTime() {
		return spawnTime;
	}

	public void setSpawnTime(double spawnTime) {
		this.spawnTime = spawnTime;
	}

	/**
	 * The attribute spawnTime describes the time the event occurs.
	 */
	Double spawnTime = 0.0;

}
