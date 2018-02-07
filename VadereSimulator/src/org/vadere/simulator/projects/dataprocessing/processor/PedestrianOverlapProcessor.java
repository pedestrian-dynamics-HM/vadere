package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesPedestrianOverlapProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
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
        setAttributes(new AttributesPedestrianOverlapProcessor());
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        Map<Integer, VPoint> pedPosMap = state.getPedestrianPositionMap();

        pedPosMap.entrySet().forEach(entry -> this.putValue(new TimestepPedestrianIdKey(state.getStep(), entry.getKey()), this.calculateOverlaps(pedPosMap, entry.getValue())));
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
        AttributesPedestrianOverlapProcessor att = (AttributesPedestrianOverlapProcessor) this.getAttributes();

        this.pedRadius = att.getPedRadius();
    }

    private int calculateOverlaps(final Map<Integer, VPoint> pedPosMap, VPoint pos) {
        return (int) pedPosMap.values().stream().filter(pedPos -> pedPos.distance(pos) < 2*this.pedRadius).count();
    }

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesPedestrianOverlapProcessor());
        }

        return super.getAttributes();
    }
}
