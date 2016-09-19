package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.state.attributes.processor.AttributesMeanPedestrianEvacuationTimeProcessor;

import java.util.List;
import java.util.stream.Collectors;

public class MeanPedestrianEvacuationTimeProcessor extends DataProcessor<NoDataKey, Double> {
    private PedestrianEvacuationTimeProcessor pedEvacTimeProc;

    public MeanPedestrianEvacuationTimeProcessor() {
        super("meanEvacuationTime");
    }

    @Override
    protected void doUpdate(final SimulationState state) {
        // No implementation needed, look at 'postLoop(SimulationState)'
    }

    @Override
    public void init(final ProcessorManager manager) {
        AttributesMeanPedestrianEvacuationTimeProcessor att = (AttributesMeanPedestrianEvacuationTimeProcessor) this.getAttributes();
        this.pedEvacTimeProc = (PedestrianEvacuationTimeProcessor) manager.getProcessor(att.getPedestrianEvacuationTimeProcessorId());
    }

    @Override
    public void postLoop(final SimulationState state) {
        this.pedEvacTimeProc.postLoop(state);

        List<Double> nonNans = this.pedEvacTimeProc.getValues().stream()
                .filter(val -> val != Double.NaN)
                .collect(Collectors.toList());
        int count = nonNans.size();

        this.addValue(NoDataKey.key(), count > 0
                ? nonNans.stream().reduce(0.0, (val1, val2) -> val1 + val2) / count
                : Double.NaN);
    }
}
