package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPositionKey;
import org.vadere.state.attributes.processor.AttributesFloorFieldProcessor;
import org.vadere.util.geometry.shapes.VPoint;

/**
 * @author Mario Teixeira Parente
 */

public class FloorFieldProcessor extends DataProcessor<TimestepPositionKey, Double> {
    private int targetId;

    public FloorFieldProcessor() {
        super("potential");
    }

    @Override
    protected void doUpdate(SimulationState state) {
        // First try, TODO: Implementation
        for (int x = 0; x < 50; ++x) {
            for (int y = 0; y < 50; ++y) {
                this.putValue(new TimestepPositionKey(state.getStep(), new VPoint(x, y)), 0.0);
            }
        }
    }

    @Override
    public void init(ProcessorManager manager) {
        AttributesFloorFieldProcessor att = (AttributesFloorFieldProcessor) this.getAttributes();
        this.targetId = att.getTargetId();
    }
}
