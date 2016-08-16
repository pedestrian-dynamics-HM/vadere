package org.vadere.simulator.projects.dataprocessing_mtp;

import org.vadere.simulator.control.SimulationState;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.Map;

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

        VRectangle measurementArea = this.getMeasurementArea();
        Map<PedestrianIdDataKey, VPoint> positionMap = this.pedPosProc.getPositions(new TimestepDataKey(step));

        int pedCount = 0;
        double sumVelocities = 0.0;

        for (Map.Entry<PedestrianIdDataKey, VPoint> entry : positionMap.entrySet()) {
            PedestrianIdDataKey pedId = entry.getKey();
            VPoint pos = entry.getValue();

            if (this.getMeasurementArea().contains(pos)) {
                sumVelocities += this.pedVelProc.getValue(new TimestepPedestrianIdDataKey(step, pedId.getKey()));
                pedCount++;
            }
        }

        this.setValue(new TimestepDataKey(step), sumVelocities / pedCount);
    }

    @Override
    void init(final AttributesProcessor attributes, final ProcessorManager factory) {
        AttributesAreaSpeedProcessor att = (AttributesAreaSpeedProcessor) attributes;
        this.pedPosProc = (PedestrianPositionProcessor) factory.getProcessor(att.getPedestrianPositionProcessorId());
        this.pedVelProc = (PedestrianVelocityProcessor) factory.getProcessor(att.getPedestrianVelocityProcessorId());

        super.init(attributes, factory);
    }
}
