package org.vadere.state.scenario.distribution.registry;

import org.vadere.state.scenario.distribution.VadereDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
public class RegisteredDistribution {
	public RegisteredDistribution(Class<?> parameter, Class<? extends VadereDistribution<?>> distribution) {
		this.parameter = parameter;
		this.distribution = distribution;
	}

	private Class<?> parameter;
	private Class<? extends VadereDistribution<?>> distribution;

	public Class<? extends VadereDistribution<?>> getDistribution() {
		return distribution;
	}

	public Class<?> getParameter() {
		return parameter;
	}
}
