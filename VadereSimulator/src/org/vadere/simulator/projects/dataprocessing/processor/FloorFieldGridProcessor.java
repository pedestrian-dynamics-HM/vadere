package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepRowKey;
import org.vadere.state.attributes.processor.AttributesFloorFieldGridProcessor;
import org.vadere.util.data.FloorFieldGridRow;

public class FloorFieldGridProcessor extends DataProcessor<TimestepRowKey, FloorFieldGridRow> {
    private int targetId;

    @Override
    protected void doUpdate(SimulationState state) {
        // First try, TODO: Implementation
        for (int i = 0; i < 50; ++i) {
            this.addValue(new TimestepRowKey(state.getStep(), i), new FloorFieldGridRow(50));
        }
    }

    @Override
    public void init(ProcessorManager manager) {
        AttributesFloorFieldGridProcessor att = (AttributesFloorFieldGridProcessor) this.getAttributes();
        this.targetId = att.getTargetId();
    }

    @Override
    public String[] toStrings(TimestepRowKey key) {
        return this.getValue(key).toStrings();
    }
}
