package org.vadere.simulator.projects.dataprocessing.processor;

import java.util.List;
import java.util.stream.Collectors;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;

/**
 * PedestrianStateProcessor adds a column "state" to the output with the state of a pedestrian.
 * 
 * The possible states are:
 *           - c = Pedestrian was created
 *           - m = Pedestrian has moved
 *           - d = Pedestrian was deleted (has reached its final target)
 * 
 * @author  Florian Künzner
 *
 */
@DataProcessorClass()
public class PedestrianStateProcessor extends DataProcessor<TimestepPedestrianIdKey, String> {

	public PedestrianStateProcessor() {
		super("state");
	}
	
	@Override
	public void init(ProcessorManager manager) {
		super.init(manager);
	}
	
	@Override
	protected void doUpdate(SimulationState state) {
		List<Integer> pedsInThisState = state.getTopography().getElements(Pedestrian.class).stream()
			.map(Agent::getId)
			.collect(Collectors.toList());
		
		//insertVertex ped states c = created or m = moved
		pedsInThisState.stream()
			.forEach(id -> {
				boolean pedEntryExists = getKeys().stream()
					.filter(ped -> ped.getPedestrianId().equals(id))
					.count() > 0;
					
				String pedState = pedEntryExists ? "m" : "c";
				this.putValue(new TimestepPedestrianIdKey(state.getStep(), id), pedState);
			});
		
		//detect deleted peds and inserts states d = deleted
		int previousStep = state.getStep() - 1;
		List<Integer> activePedsInPreviousState = getKeys().stream()
			.filter(key -> key.getTimestep() == previousStep)
			.filter(key -> !getValue(key).equals("d"))
			.map(TimestepPedestrianIdKey::getPedestrianId)
			.collect(Collectors.toList());
		
		List<Integer> deletedPeds = activePedsInPreviousState.stream()
			.filter(id -> !pedsInThisState.contains(id))
			.collect(Collectors.toList());
		
		deletedPeds.stream()
			.forEach(id -> this.putValue(new TimestepPedestrianIdKey(previousStep, id), "d"));
	}
}
