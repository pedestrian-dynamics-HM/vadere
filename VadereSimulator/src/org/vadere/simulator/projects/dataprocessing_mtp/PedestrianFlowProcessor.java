package org.vadere.simulator.projects.dataprocessing_mtp;

import org.vadere.simulator.control.SimulationState;

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

            this.setValue(key, velocity * density);
        }
    }

    @Override
    void init(final AttributesProcessor attributes, final ProcessorManager factory) {
        AttributesPedestrianFlowProcessor att = (AttributesPedestrianFlowProcessor) attributes;

        this.pedVelProc = (PedestrianVelocityProcessor) factory.getProcessor(att.getPedestrianVelocityProcessorId());
        this.pedDensProc = (PedestrianDensityProcessor) factory.getProcessor(att.getPedestrianDensityProcessorId());
    }
}
