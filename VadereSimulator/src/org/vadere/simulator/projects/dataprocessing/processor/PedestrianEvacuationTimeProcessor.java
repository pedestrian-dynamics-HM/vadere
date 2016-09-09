package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.datakey.PedestrianIdDataKey;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.state.attributes.processor.AttributesPedestrianEvacuationTimeProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;

public class PedestrianEvacuationTimeProcessor extends Processor<PedestrianIdDataKey, Double> {
	private PedestrianStartTimeProcessor pedStTimeProc;

	public PedestrianEvacuationTimeProcessor() {
		super("tevac");
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		this.pedStTimeProc.update(state);

		state.getTopography().getElements(Pedestrian.class).stream()
				.map(ped -> new PedestrianIdDataKey(ped.getId()))
				.forEach(key -> this.addValue(key, state.getSimTimeInSec() - this.pedStTimeProc.getValue(key)));
	}

	@Override
	public void postLoop(final SimulationState state) {
		state.getTopography().getElements(Pedestrian.class).stream()
				.map(ped -> new PedestrianIdDataKey(ped.getId()))
				.forEach(key -> this.addValue(key, Double.NaN));
	}

	@Override
	public void init(final AttributesProcessor attributes, final ProcessorManager manager) {
		AttributesPedestrianEvacuationTimeProcessor att = (AttributesPedestrianEvacuationTimeProcessor) attributes;
		this.pedStTimeProc = (PedestrianStartTimeProcessor) manager.getProcessor(att.getPedestrianStartTimeProcessorId());
	}
}
