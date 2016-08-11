package org.vadere.simulator.projects.dataprocessing_mtp;

import org.vadere.simulator.control.SimulationState;

public class PedestrianEvacuationTimeProcessor extends Processor<PedestrianIdDataKey, Double> {
	private PedestrianStartTimeProcessor pedStTimeProc;
	private PedestrianEndTimeProcessor pedEndTimeProc;

	public PedestrianEvacuationTimeProcessor() {
		super("tevac");
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		this.pedStTimeProc.update(state);
		this.pedEndTimeProc.update(state);

		this.pedEndTimeProc.getKeys().forEach(
				key -> this.setValue(key, this.pedEndTimeProc.getValue(key) - this.pedStTimeProc.getValue(key)));
	}

	@Override
	void init(final AttributesProcessor attributes, final ProcessorFactory factory) {
		// TODO [priority=medium] [task=check] is initialization needed?
	}
}
