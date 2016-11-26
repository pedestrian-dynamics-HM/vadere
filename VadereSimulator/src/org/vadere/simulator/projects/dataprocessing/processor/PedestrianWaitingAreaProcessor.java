package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.state.attributes.processor.AttributesPedestrianWaitingTimeProcessor;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Map;

/**
 * Counts the number of agents in the given waiting area VShape.
 * 
 * @author Felix Dietrich
 *
 */

public class PedestrianWaitingAreaProcessor extends DataProcessor<TimestepKey, Integer> {
    private VRectangle waitingArea;

    public PedestrianWaitingAreaProcessor() {
        super("waitingArea");
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        Map<Integer, VPoint> pedPosMap = state.getPedestrianPositionMap();

        int agentCounter = 0;
        for (Map.Entry<Integer, VPoint> entry : pedPosMap.entrySet()) {
            VPoint pos = entry.getValue();

            if (this.waitingArea.contains(pos)) {
                agentCounter++;
            }
        }
        TimestepKey key = new TimestepKey(state.getStep());
        this.putValue(key, agentCounter);
    }

    @Override
    public void init(final ProcessorManager manager) {
        AttributesPedestrianWaitingTimeProcessor att = (AttributesPedestrianWaitingTimeProcessor) this.getAttributes();
        this.waitingArea = att.getWaitingArea();
    }
}
