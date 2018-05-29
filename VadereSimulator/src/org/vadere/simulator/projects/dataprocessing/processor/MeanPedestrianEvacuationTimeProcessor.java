package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.state.attributes.processor.AttributesMeanPedestrianEvacuationTimeProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Mario Teixeira Parente
 */

public class MeanPedestrianEvacuationTimeProcessor extends DataProcessor<NoDataKey, Double> {
	private PedestrianEvacuationTimeProcessor pedEvacTimeProc;

	public MeanPedestrianEvacuationTimeProcessor() {
		super("meanEvacuationTime");
		setAttributes(new AttributesMeanPedestrianEvacuationTimeProcessor());
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		//ensure that all required DataProcessors are updated.
		this.pedEvacTimeProc.doUpdate(state);
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesMeanPedestrianEvacuationTimeProcessor att = (AttributesMeanPedestrianEvacuationTimeProcessor) this.getAttributes();
		this.pedEvacTimeProc = (PedestrianEvacuationTimeProcessor) manager.getProcessor(att.getPedestrianEvacuationTimeProcessorId());
	}

	@Override
	public void postLoop(final SimulationState state) {
		this.pedEvacTimeProc.postLoop(state);

		List<Double> nonNans = this.pedEvacTimeProc.getValues().stream()
				.filter(val -> !val.isNaN())
				.collect(Collectors.toList());
		int count = nonNans.size();

		this.putValue(NoDataKey.key(), count > 0
				? nonNans.parallelStream().reduce(0.0, (val1, val2) -> val1 + val2) / count
				: Double.NaN);
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesMeanPedestrianEvacuationTimeProcessor());
		}

		return super.getAttributes();
	}
}
