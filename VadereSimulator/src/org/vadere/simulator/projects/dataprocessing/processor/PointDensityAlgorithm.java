package org.vadere.simulator.projects.dataprocessing.processor;

public abstract class PointDensityAlgorithm implements IPointDensityAlgorithm {
	private String name;

	protected PointDensityAlgorithm(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}
}
