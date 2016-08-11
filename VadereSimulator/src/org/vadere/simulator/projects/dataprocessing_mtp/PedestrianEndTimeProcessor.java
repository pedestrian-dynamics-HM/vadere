package org.vadere.simulator.projects.dataprocessing_mtp;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.scenario.Pedestrian;

public class PedestrianEndTimeProcessor extends Processor<PedestrianIdDataKey, Double> {
	private PedestrianStartTimeProcessor pedStTimeProc;

	public PedestrianEndTimeProcessor() {
		super("tend");

		this.pedStTimeProc = pedStTimeProc;
	}

	@Override
	protected void doUpdate(final SimulationState state) {
		this.pedStTimeProc.update(state);

		Set<PedestrianIdDataKey> stTimeKeys = new HashSet<>();
		stTimeKeys.addAll(this.pedStTimeProc.getKeys()); // Create a copy of start time keys

		Set<PedestrianIdDataKey> currentKeys = state.getTopography().getElements(Pedestrian.class).stream()
				.map(ped -> new PedestrianIdDataKey(ped.getId())).collect(Collectors.toSet());
		Set<PedestrianIdDataKey> endTimeKeys = this.getKeys();

		// Find out which pedestrians have been exited in this step
		stTimeKeys.removeAll(currentKeys);
		stTimeKeys.removeAll(endTimeKeys);

		stTimeKeys.forEach(key -> this.setValue(key, state.getSimTimeInSec()));
	}

	@Override
	void init(final AttributesProcessor attributes, final ProcessorFactory factory) {
		// TODO [priority=medium] [task=check] is initialization needed?
	}
}
