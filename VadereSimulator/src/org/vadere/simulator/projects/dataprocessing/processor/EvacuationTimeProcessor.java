package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.state.attributes.processor.AttributesEvacuationTimeProcessor;
import org.vadere.state.scenario.Pedestrian;

import java.util.Collection;
import java.util.Collections;

public class EvacuationTimeProcessor extends DataProcessor<NoDataKey, Double> {
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
    public void init(final ProcessorManager manager) {
        AttributesEvacuationTimeProcessor att = (AttributesEvacuationTimeProcessor) this.getAttributes();
        this.pedEvacTimeProc = (PedestrianEvacuationTimeProcessor) manager.getProcessor(att.getPedestrianEvacuationTimeProcessorId());
    }
}
