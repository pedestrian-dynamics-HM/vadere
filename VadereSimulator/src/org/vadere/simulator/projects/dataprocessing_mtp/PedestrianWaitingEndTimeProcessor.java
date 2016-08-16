package org.vadere.simulator.projects.dataprocessing_mtp;

import org.vadere.simulator.control.SimulationState;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Map;

public class PedestrianWaitingEndTimeProcessor extends Processor<PedestrianIdDataKey, Double> {
    private VRectangle waitingArea;

    public PedestrianWaitingEndTimeProcessor() {
        super("twaitend");
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        Map<Integer, VPoint> pedPosMap = state.getPedestrainPositionMap();

        for (Map.Entry<Integer, VPoint> entry : pedPosMap.entrySet()) {
            int pedId = entry.getKey();
            VPoint pos = entry.getValue();

            if (this.waitingArea.contains(pos)) {
                PedestrianIdDataKey key = new PedestrianIdDataKey(pedId);
                this.setValue(key, state.getSimTimeInSec());
            }
        }
    }

    @Override
    void init(final AttributesProcessor attributes, final ProcessorManager manager) {
        AttributesPedestrianWaitingEndTimeProcessor att = (AttributesPedestrianWaitingEndTimeProcessor) attributes;
        this.waitingArea = att.getWaitingArea();
    }
}
