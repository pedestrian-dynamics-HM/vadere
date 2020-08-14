package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.state.attributes.processor.AttributesFlowProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

import java.util.Collection;
import java.util.OptionalDouble;

/**
 * @author Marion Goedel
 *
 */
@DataProcessorClass()
public class FlowProcessor extends NoDataKeyProcessor<Double> {
    private PedestrianLineCrossProcessor pedLineCross;

    public FlowProcessor() {
        super("flow");
        setAttributes(new AttributesFlowProcessor());
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        this.pedLineCross.update(state);
    }


    @Override
    public void postLoop(final SimulationState state) {
        pedLineCross.postLoop(state);
        Collection<Double> lineCrossValues = pedLineCross.getValues();
        OptionalDouble lineCrossMax = lineCrossValues.stream().mapToDouble(Double::doubleValue).max();
        OptionalDouble lineCrossMin = lineCrossValues.stream().mapToDouble(Double::doubleValue).min();
        double flow;
        if(lineCrossMax.isPresent() && lineCrossMin.isPresent()){
            double deltaT = lineCrossMax.getAsDouble() - lineCrossMin.getAsDouble();
            double deltaN = lineCrossValues.size();
            // double b = pedLineCross.getLine().length(); // width of corridor -> specific flow
            flow = deltaN/deltaT;
        } else {
            flow = -1.0;
        }
        putValue(NoDataKey.key(), flow);
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
        AttributesFlowProcessor att = (AttributesFlowProcessor) this.getAttributes();
        this.pedLineCross = (PedestrianLineCrossProcessor) manager.getProcessor(att.getPedestrianLineCrossProcessorId());
    }

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesFlowProcessor());
        }

        return super.getAttributes();
    }
}
