package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;

/**
 * @author Mario Teixeira Parente
 *
 */

public abstract class AreaDensityProcessor extends AreaDataProcessor<Double> {
    private IAreaDensityAlgorithm densAlg;

    protected void setAlgorithm(final IAreaDensityAlgorithm densAlg) {
        this.densAlg = densAlg;
        this.setHeaders(this.densAlg.getName() + "Density");
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        this.putValue(new TimestepKey(state.getStep()), this.densAlg.getDensity(state));
    }
}
