package org.vadere.simulator.projects.dataprocessing.processor;

/**
 * @author Mario Teixeira Parente
 *
 */

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
