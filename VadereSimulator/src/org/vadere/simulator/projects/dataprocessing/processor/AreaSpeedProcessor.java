package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepKey;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesAreaSpeedProcessor;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Map;

/**
 * @author Mario Teixeira Parente
 *
 */

public class AreaSpeedProcessor extends AreaDataProcessor<Double> {
    private PedestrianPositionProcessor pedPosProc;
    private PedestrianVelocityProcessor pedVelProc;

    public AreaSpeedProcessor() {
        super("areaSpeed");
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        int step = state.getStep();

        this.pedPosProc.update(state);
        this.pedVelProc.update(state);

        Map<PedestrianIdKey, VPoint> positionMap = this.pedPosProc.getPositions(new TimestepKey(step));

        int pedCount = 0;
        double sumVelocities = 0.0;

        for (Map.Entry<PedestrianIdKey, VPoint> entry : positionMap.entrySet()) {
            final int pedId = entry.getKey().getPedestrianId();
            final VPoint pos = entry.getValue();

            if (getMeasurementArea().contains(pos)) {
                sumVelocities += this.pedVelProc.getValue(new TimestepPedestrianIdKey(step, pedId));
                pedCount++;
            }
        }

        this.putValue(new TimestepKey(step), sumVelocities / pedCount);
    }

    @Override
    public void init(final ProcessorManager manager) {
        AttributesAreaSpeedProcessor att = (AttributesAreaSpeedProcessor) this.getAttributes();
        this.pedPosProc = (PedestrianPositionProcessor) manager.getProcessor(att.getPedestrianPositionProcessorId());
        this.pedVelProc = (PedestrianVelocityProcessor) manager.getProcessor(att.getPedestrianVelocityProcessorId());

        super.init(manager);
    }
}
