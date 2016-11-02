package org.vadere.simulator.projects.dataprocessing.processor;

import java.util.List;
import java.util.stream.Collectors;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;

/**
 * @brief   PedestrianStateProcessor adds a column "state" to the output with the state of a pedestrian.
 * 
 * @details The possible states are:
 *           - c = Pedestrian was created
 *           - m = Pedestrian has moved
 *           - d = Pedestrian was deleted (has reached its final target)
 * 
 * @author  Florian Künzner
 *
 */
public class PedestrianStateProcessor extends DataProcessor<TimestepPedestrianIdKey, String> {

	public PedestrianStateProcessor() {
		super("state");
	}
	
	@Override
	public void init(ProcessorManager manager) {
		// No initialization needed		
	}
	
	@Override
	protected void doUpdate(SimulationState state) {
		List<Integer> pedsInThisState = state.getTopography().getElements(Pedestrian.class).stream()
			.map(ped -> ped.getId())
			.collect(Collectors.toList());
		
		//insert ped states c = created or m = moved 
		pedsInThisState.stream()
			.forEach(id -> {
				boolean pedEntryExists = getKeys().stream()
					.filter(ped -> ped.getPedestrianId() == id)
					.count() > 0;
					
				String pedState = pedEntryExists ? "m" : "c";
				this.putValue(new TimestepPedestrianIdKey(state.getStep(), id), pedState);
			});
		
		//detect deleted peds and inserts states d = deleted
		int previousStep = state.getStep() - 1;
		List<Integer> activePedsInPreviousState = getKeys().stream()
			.filter(key -> key.getTimestep() == previousStep)
			.filter(key -> getValue(key) != "d")
			.map(TimestepPedestrianIdKey::getPedestrianId)
			.collect(Collectors.toList());
		
		List<Integer> deletedPeds = activePedsInPreviousState.stream()
			.filter(id -> !pedsInThisState.contains(id))
			.collect(Collectors.toList());
		
		deletedPeds.stream()
			.forEach(id -> this.putValue(new TimestepPedestrianIdKey(previousStep, id), "d"));
	}
}
