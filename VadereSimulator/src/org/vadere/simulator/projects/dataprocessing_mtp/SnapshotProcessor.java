package org.vadere.simulator.projects.dataprocessing_mtp;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.io.JsonConverter;

public class SnapshotProcessor extends Processor<NoDataKey, String> {
    public SnapshotProcessor() {
        super("");
    }

    @Override
    public void preLoop(SimulationState state) {
        try {
            this.addValue(NoDataKey.key(), JsonConverter.serializeSimulationStateSnapshot(state, true));
        }
        catch (JsonProcessingException ex) {
            this.addValue(NoDataKey.key(), ex.getMessage());
        }
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        // No update needed
    }

    @Override
    public void init(final AttributesProcessor attributes, final ProcessorManager manager) {
        // No initialization needed
    }
}
