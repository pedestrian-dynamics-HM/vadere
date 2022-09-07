package org.vadere.state.scenario.distribution;

import org.apache.commons.math3.random.RandomGenerator;
import org.vadere.state.attributes.distributions.AttributesDistribution;
import org.vadere.util.AttributesAttached;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu), Lukas Gradl (lgradl@hm.edu)
 */
public abstract class VDistribution<T extends AttributesDistribution> extends AttributesAttached<T>{

	public VDistribution(T parameter,RandomGenerator randomGenerator) throws Exception {
		setValues(parameter, randomGenerator);
	}
    public VDistribution() {}
    abstract protected void setValues(T parameter,RandomGenerator randomGenerator) throws Exception;

	abstract public double getNextSpawnTime(double timeCurrentEvent);

}
