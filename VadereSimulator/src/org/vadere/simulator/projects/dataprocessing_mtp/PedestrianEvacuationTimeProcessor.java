package org.vadere.simulator.projects.dataprocessing_mtp;

import org.vadere.simulator.control.SimulationState;
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
				.forEach(key -> this.setValue(key, state.getSimTimeInSec() - this.pedStTimeProc.getValue(key)));
	}

	@Override
	public void postLoop(final SimulationState state) {
		state.getTopography().getElements(Pedestrian.class).stream()
				.map(ped -> new PedestrianIdDataKey(ped.getId()))
				.forEach(key -> this.setValue(key, Double.NaN));
	}

	@Override
	void init(final AttributesProcessor attributes, final ProcessorFactory factory) {
		AttributesPedestrianEvacuationTimeProcessor att = (AttributesPedestrianEvacuationTimeProcessor) attributes;
		this.pedStTimeProc = (PedestrianStartTimeProcessor) factory.getProcessor(att.getPedestrianStartTimeProcessorId());
	}
}
