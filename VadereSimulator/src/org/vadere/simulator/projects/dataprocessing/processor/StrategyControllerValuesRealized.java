package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;

/**
 * @author Christina Mayr
 */
@DataProcessorClass()
public class StrategyControllerValuesRealized extends StrategyDataProcessor<Double> {


    public StrategyControllerValuesRealized() {
        super("RealizedValues");
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        this.putValue(new TimestepKey(state.getStep()), getStrategyModelOutput(state));
    }
}
