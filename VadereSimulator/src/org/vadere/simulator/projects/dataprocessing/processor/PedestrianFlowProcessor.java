package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.attributes.processor.AttributesPedestrianFlowProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Mario Teixeira Parente
 *
 */
@DataProcessorClass()
public class PedestrianFlowProcessor extends DataProcessor<TimestepPedestrianIdKey, Double> {
    private PedestrianVelocityProcessor pedVelProc;
    private PedestrianDensityProcessor pedDensProc;

    public PedestrianFlowProcessor() {
        super("flow");
        setAttributes(new AttributesPedestrianFlowProcessor());
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        this.pedVelProc.update(state);
        this.pedDensProc.update(state);

        Set<TimestepPedestrianIdKey> keys = this.pedVelProc.getKeys().stream().filter(key -> key.getTimestep() == state.getStep()).collect(Collectors.toSet());

        for (TimestepPedestrianIdKey key : keys) {
            double velocity = this.pedVelProc.getValue(key);
            double density = this.pedDensProc.getValue(key);

            this.putValue(key, velocity * density);
        }
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
        AttributesPedestrianFlowProcessor att = (AttributesPedestrianFlowProcessor) this.getAttributes();

        this.pedVelProc = (PedestrianVelocityProcessor) manager.getProcessor(att.getPedestrianVelocityProcessorId());
        this.pedDensProc = (PedestrianDensityProcessor) manager.getProcessor(att.getPedestrianDensityProcessorId());
    }

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesPedestrianFlowProcessor());
        }

        return super.getAttributes();
    }
}
