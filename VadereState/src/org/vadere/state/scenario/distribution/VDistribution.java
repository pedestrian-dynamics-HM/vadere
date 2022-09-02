package org.vadere.state.scenario.distribution;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.util.AttributesAttached;
import org.vadere.state.attributes.distributions.AttributesDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
public abstract class VDistribution<T extends AttributesDistribution> implements AttributesAttached{

	public VDistribution(T parameter, int spawnNumber, RandomGenerator randomGenerator) throws Exception {
		setValues(parameter, spawnNumber, randomGenerator);
	}
    public VDistribution() {}
    abstract protected void setValues(T parameter, int spawnNumber, RandomGenerator randomGenerator) throws Exception;

	abstract public double getNextSpawnTime(double timeCurrentEvent);

}
