package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdDataKey;
import org.vadere.state.attributes.processor.AttributesPedestrianFlowProcessor;

import java.util.Set;
import java.util.stream.Collectors;

public class PedestrianFlowProcessor extends Processor<TimestepPedestrianIdDataKey, Double> {
    private PedestrianVelocityProcessor pedVelProc;
    private PedestrianDensityProcessor pedDensProc;

    public PedestrianFlowProcessor() {
        super("flow");
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        this.pedVelProc.update(state);
        this.pedDensProc.update(state);

        Set<TimestepPedestrianIdDataKey> keys = this.pedVelProc.getKeys().stream().filter(key -> key.getTimestep() == state.getStep()).collect(Collectors.toSet());

        for (TimestepPedestrianIdDataKey key : keys) {
            double velocity = this.pedVelProc.getValue(key);
            double density = this.pedDensProc.getValue(key);

            this.addValue(key, velocity * density);
        }
    }

    @Override
    public void init(final ProcessorManager manager) {
        AttributesPedestrianFlowProcessor att = (AttributesPedestrianFlowProcessor) this.getAttributes();

        this.pedVelProc = (PedestrianVelocityProcessor) manager.getProcessor(att.getPedestrianVelocityProcessorId());
        this.pedDensProc = (PedestrianDensityProcessor) manager.getProcessor(att.getPedestrianDensityProcessorId());
    }
}
