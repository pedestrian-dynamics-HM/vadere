package org.vadere.simulator.projects.dataprocessing_mtp;

import org.vadere.simulator.control.SimulationState;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Map;

public class PedestrianWaitingTimeProcessor extends Processor<PedestrianIdDataKey, Double> {
    private double lastSimTime;
    private VRectangle waitingArea;

    public PedestrianWaitingTimeProcessor() {
        super("twait");

        this.lastSimTime = 0.0;
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        Map<Integer, VPoint> pedPosMap = state.getPedestrainPositionMap();

        double dt = state.getSimTimeInSec() - this.lastSimTime;

        for (Map.Entry<Integer, VPoint> entry : pedPosMap.entrySet()) {
            int pedId = entry.getKey();
            VPoint pos = entry.getValue();

            if (this.waitingArea.contains(pos)) {
                PedestrianIdDataKey key = new PedestrianIdDataKey(pedId);
                this.setValue(key, (this.hasValue(key) ? this.getValue(key) : 0.0) + dt);
            }
        }

        this.lastSimTime = state.getSimTimeInSec();
    }

    @Override
    void init(final AttributesProcessor attributes, final ProcessorManager factory) {
        AttributesPedestrianWaitingTimeProcessor att = (AttributesPedestrianWaitingTimeProcessor) attributes;
        this.waitingArea = att.getWaitingArea();
    }
}
