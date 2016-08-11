package org.vadere.simulator.projects.dataprocessing_mtp;

public abstract class PedestrianDensityAlgorithm implements IPedestrianDensityAlgorithm {
	private String name;

	protected PedestrianDensityAlgorithm(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}
}
