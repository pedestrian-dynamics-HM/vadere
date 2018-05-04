package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.state.attributes.processor.AttributesEvacuationTimeProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

import java.util.Collections;

/**
 * @author Mario Teixeira Parente
 *
 */
@DataProcessorClass()
public class EvacuationTimeProcessor extends DataProcessor<NoDataKey, Double> {
    private PedestrianEvacuationTimeProcessor pedEvacTimeProc;

    public EvacuationTimeProcessor() {
        super("evacuationTime");
        setAttributes(new AttributesEvacuationTimeProcessor());
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        this.pedEvacTimeProc.update(state);
    }

    @Override
    public void postLoop(final SimulationState state) {
        this.pedEvacTimeProc.postLoop(state);

        double result = 0.0;

        if (this.pedEvacTimeProc.getValues().size() > 0) {
            result = this.pedEvacTimeProc.getValues().stream().anyMatch(tevac -> tevac == Double.NaN)
                    ? Double.NaN
                    : Collections.max(this.pedEvacTimeProc.getValues());
        }

        this.putValue(NoDataKey.key(), result);
    }

    @Override
    public void init(final ProcessorManager manager) {
        super.init(manager);
        AttributesEvacuationTimeProcessor att = (AttributesEvacuationTimeProcessor) this.getAttributes();
        this.pedEvacTimeProc = (PedestrianEvacuationTimeProcessor) manager.getProcessor(att.getPedestrianEvacuationTimeProcessorId());
    }

    @Override
    public AttributesProcessor getAttributes() {
        if(super.getAttributes() == null) {
            setAttributes(new AttributesEvacuationTimeProcessor());
        }

        return super.getAttributes();
    }
}
