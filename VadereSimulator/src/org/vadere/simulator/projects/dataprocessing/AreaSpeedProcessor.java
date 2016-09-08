package org.vadere.simulator.projects.dataprocessing;

import java.util.Map;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.attributes.processors.AttributesAreaSpeedProcessor;
import org.vadere.state.attributes.processors.AttributesProcessor;
import org.vadere.util.geometry.shapes.VPoint;

public class AreaSpeedProcessor extends AreaProcessor<Double> {
    private PedestrianPositionProcessor pedPosProc;
    private PedestrianVelocityProcessor pedVelProc;

    public AreaSpeedProcessor() {
        this.setHeader("area-speed");
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        int step = state.getStep();

        this.pedPosProc.update(state);
        this.pedVelProc.update(state);

        Map<PedestrianIdDataKey, VPoint> positionMap = this.pedPosProc.getPositions(new TimestepDataKey(step));

        int pedCount = 0;
        double sumVelocities = 0.0;

        for (Map.Entry<PedestrianIdDataKey, VPoint> entry : positionMap.entrySet()) {
            final int pedId = entry.getKey().getPedestrianId();
            final VPoint pos = entry.getValue();

            if (getMeasurementArea().contains(pos)) {
                sumVelocities += this.pedVelProc.getValue(new TimestepPedestrianIdDataKey(step, pedId));
                pedCount++;
            }
        }

        this.addValue(new TimestepDataKey(step), sumVelocities / pedCount);
    }

    @Override
    public void init(final AttributesProcessor attributes, final ProcessorManager manager) {
        AttributesAreaSpeedProcessor att = (AttributesAreaSpeedProcessor) attributes;
        this.pedPosProc = (PedestrianPositionProcessor) manager.getProcessor(att.getPedestrianPositionProcessorId());
        this.pedVelProc = (PedestrianVelocityProcessor) manager.getProcessor(att.getPedestrianVelocityProcessorId());

        super.init(attributes, manager);
    }
}
