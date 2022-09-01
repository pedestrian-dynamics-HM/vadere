package org.vadere.state.scenario.distribution.registry;

import org.vadere.state.scenario.distribution.VDistribution;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
public class RegisteredDistribution {
	public RegisteredDistribution(Class<?> parameter, Class<? extends VDistribution<?>> distribution) {
		this.parameter = parameter;
		this.distribution = distribution;
	}

	private Class<?> parameter;
	private Class<? extends VDistribution<?>> distribution;

	public Class<? extends VDistribution<?>> getDistribution() {
		return distribution;
	}

	public Class<?> getParameter() {
		return parameter;
	}
}
