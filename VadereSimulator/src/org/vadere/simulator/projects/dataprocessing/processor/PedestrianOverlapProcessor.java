package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesPedestrianOverlapProcessor;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Map;

/**
 * @author Mario Teixeira Parente
 *
 */

public class PedestrianOverlapProcessor extends DataProcessor<TimestepPedestrianIdKey, Integer> {
    private double pedRadius;

    public PedestrianOverlapProcessor() {
        super("overlaps");
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        Map<Integer, VPoint> pedPosMap = state.getPedestrianPositionMap();

        pedPosMap.entrySet().forEach(entry -> this.addValue(new TimestepPedestrianIdKey(state.getStep(), entry.getKey()), this.calculateOverlaps(pedPosMap, entry.getValue())));
    }

    @Override
    public void init(final ProcessorManager manager) {
        AttributesPedestrianOverlapProcessor att = (AttributesPedestrianOverlapProcessor) this.getAttributes();

        this.pedRadius = att.getPedRadius();
    }

    private int calculateOverlaps(final Map<Integer, VPoint> pedPosMap, VPoint pos) {
        return (int) pedPosMap.values().stream().filter(pedPos -> pedPos.distance(pos) < 2*this.pedRadius).count();
    }
}
