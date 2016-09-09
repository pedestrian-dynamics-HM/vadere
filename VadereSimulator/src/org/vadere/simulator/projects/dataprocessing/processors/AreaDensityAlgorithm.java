package org.vadere.simulator.projects.dataprocessing.processors;

public abstract class AreaDensityAlgorithm implements IAreaDensityAlgorithm {
    private String name;

    public AreaDensityAlgorithm(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
