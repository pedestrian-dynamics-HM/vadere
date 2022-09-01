package org.vadere.state.scenario.distribution;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.AttributesAttached;
import org.vadere.state.attributes.distributions.AttributesDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
public abstract class VDistribution<T extends AttributesDistribution> implements AttributesAttached {

	public VDistribution(T parameter, int spawnNumber, RandomGenerator randomGenerator) throws Exception {
		setValues(parameter, spawnNumber, randomGenerator);
	}

	abstract protected void setValues(T parameter, int spawnNumber, RandomGenerator randomGenerator) throws Exception;

	abstract public int getSpawnNumber(double timeCurrentEvent);

	abstract public double getNextSpawnTime(double timeCurrentEvent);

	abstract public int getRemainingSpawnAgents();

	abstract public void setRemainingSpawnAgents(int remainingAgents);

}
