package org.vadere.simulator.projects.dataprocessing.processor;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.control.strategy.models.IStrategyModel;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.state.attributes.processor.AttributesAreaProcessor;

/**
 * @author Christina Mayr
 *
 */

public abstract class StrategyDataProcessor<V> extends DataProcessor<TimestepKey, V> {


    protected StrategyDataProcessor(final String... headers) {
        super(headers);
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
    }

    public V getStrategyModelOutput(@NotNull SimulationState state) {
        return (V) state.getStrategyModel().getStrategyInfoForDataProcessor();
    }
}
