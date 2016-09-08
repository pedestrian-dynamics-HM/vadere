package org.vadere.simulator.projects.dataprocessing;

import java.util.Set;
import java.util.stream.Collectors;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.attributes.processors.AttributesPedestrianFlowProcessor;
import org.vadere.state.attributes.processors.AttributesProcessor;

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
    public void init(final AttributesProcessor attributes, final ProcessorManager manager) {
        AttributesPedestrianFlowProcessor att = (AttributesPedestrianFlowProcessor) attributes;

        this.pedVelProc = (PedestrianVelocityProcessor) manager.getProcessor(att.getPedestrianVelocityProcessorId());
        this.pedDensProc = (PedestrianDensityProcessor) manager.getProcessor(att.getPedestrianDensityProcessorId());
    }
}
