package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepDataKey;

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
        this.addValue(new TimestepDataKey(state.getStep()), this.densAlg.getDensity(state));
    }
}
