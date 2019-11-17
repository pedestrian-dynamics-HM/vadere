package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.NoDataKey;
import org.vadere.state.attributes.processor.AttributesMeanPedestrianEvacuationTimeProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;

import java.util.Collection;

/**
 * @author Mario Teixeira Parente
 */
@DataProcessorClass()
public class MeanPedestrianEvacuationTimeProcessor extends NoDataKeyProcessor<Double> {
	private PedestrianEvacuationTimeProcessor pedEvacTimeProc;

	public MeanPedestrianEvacuationTimeProcessor() {
		super("meanEvacuationTime");
		setAttributes(new AttributesMeanPedestrianEvacuationTimeProcessor());
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		//ensure that all required DataProcessors are updated.
		this.pedEvacTimeProc.update(state);
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
		AttributesMeanPedestrianEvacuationTimeProcessor att = (AttributesMeanPedestrianEvacuationTimeProcessor) this.getAttributes();
		this.pedEvacTimeProc = (PedestrianEvacuationTimeProcessor) manager.getProcessor(att.getPedestrianEvacuationTimeProcessorId());
	}

	@Override
	public void postLoop(final SimulationState state) {
		pedEvacTimeProc.postLoop(state);
		Collection<Double> evacTimes = pedEvacTimeProc.getValues();
		putValue(NoDataKey.key(), evacTimes.parallelStream().reduce(0.0, (val1, val2) -> val1 + val2) / evacTimes.size());
	}

	@Override
	public AttributesProcessor getAttributes() {
		if (super.getAttributes() == null) {
			setAttributes(new AttributesMeanPedestrianEvacuationTimeProcessor());
		}

		return super.getAttributes();
	}
}
