package org.vadere.simulator.projects.dataprocessing;

import java.util.Collection;
import java.util.Collections;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.attributes.processors.AttributesEvacuationTimeProcessor;
import org.vadere.state.attributes.processors.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;

public class EvacuationTimeProcessor extends Processor<NoDataKey, Double> {
    private PedestrianEvacuationTimeProcessor pedEvacTimeProc;

    public EvacuationTimeProcessor() {
        super("tevac");
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        // No implementation needed, look at 'postLoop(SimulationState)'
    }

    @Override
    public void postLoop(final SimulationState state) {
        this.pedEvacTimeProc.postLoop(state);

        Collection<Pedestrian> peds = state.getTopography().getElements(Pedestrian.class);

        this.addValue(NoDataKey.key(), this.pedEvacTimeProc.getValues().stream().anyMatch(tevac -> tevac == Double.NaN)
                ? Double.NaN
                : Collections.max(this.pedEvacTimeProc.getValues()));
    }

    @Override
    public void init(final AttributesProcessor attributes, final ProcessorManager manager) {
        AttributesEvacuationTimeProcessor att = (AttributesEvacuationTimeProcessor) attributes;
        this.pedEvacTimeProc = (PedestrianEvacuationTimeProcessor) manager.getProcessor(att.getPedestrianEvacuationTimeProcessorId());
    }
}
