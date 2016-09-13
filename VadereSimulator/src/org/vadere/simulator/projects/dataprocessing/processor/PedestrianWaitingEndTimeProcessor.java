package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdDataKey;
import org.vadere.state.attributes.processor.AttributesPedestrianWaitingEndTimeProcessor;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Map;

public class PedestrianWaitingEndTimeProcessor extends DataProcessor<PedestrianIdDataKey, Double> {
    private VRectangle waitingArea;

    public PedestrianWaitingEndTimeProcessor() {
        super("twaitend");
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        Map<Integer, VPoint> pedPosMap = state.getPedestrianPositionMap();

        for (Map.Entry<Integer, VPoint> entry : pedPosMap.entrySet()) {
            int pedId = entry.getKey();
            VPoint pos = entry.getValue();

            if (this.waitingArea.contains(pos)) {
                PedestrianIdDataKey key = new PedestrianIdDataKey(pedId);
                this.addValue(key, state.getSimTimeInSec());
            }
        }
    }

    @Override
    public void init(final ProcessorManager manager) {
        AttributesPedestrianWaitingEndTimeProcessor att = (AttributesPedestrianWaitingEndTimeProcessor) this.getAttributes();
        this.waitingArea = att.getWaitingArea();
    }
}
