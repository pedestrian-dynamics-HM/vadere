package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesPedestrianWaitingTimeProcessor;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Map;

/**
 * @author Mario Teixeira Parente
 *
 */

public class PedestrianWaitingTimeProcessor extends DataProcessor<PedestrianIdKey, Double> {
    private double lastSimTime;
    private VRectangle waitingArea;

    public PedestrianWaitingTimeProcessor() {
        super("waitingTime");

        this.lastSimTime = 0.0;
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        Map<Integer, VPoint> pedPosMap = state.getPedestrianPositionMap();

        double dt = state.getSimTimeInSec() - this.lastSimTime;

        for (Map.Entry<Integer, VPoint> entry : pedPosMap.entrySet()) {
            int pedId = entry.getKey();
            VPoint pos = entry.getValue();

            if (this.waitingArea.contains(pos)) {
                PedestrianIdKey key = new PedestrianIdKey(pedId);
                this.setValue(key, (this.hasValue(key) ? this.getValue(key) : 0.0) + dt);
            }
        }

        this.lastSimTime = state.getSimTimeInSec();
    }

    @Override
    public void init(final ProcessorManager manager) {
        AttributesPedestrianWaitingTimeProcessor att = (AttributesPedestrianWaitingTimeProcessor) this.getAttributes();
        this.waitingArea = att.getWaitingArea();
    }
}
