package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.traci.CompoundObject;
import org.vadere.state.traci.CompoundObjectBuilder;
import org.vadere.state.traci.CompoundObjectProvider;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.util.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;

@DataProcessorClass
public class PedestrianCommandIdsReceivedTimesProcessor extends DataProcessor<TimestepPedestrianIdKey, Integer>  implements CompoundObjectProvider {

	private static Logger logger = Logger.getLogger(PedestrianGroupIDProcessor.class);
	private LinkedList<Integer> processedAgentIds;

	public PedestrianCommandIdsReceivedTimesProcessor() {
		super("commandId");
	}

	@Override
	protected void doUpdate(SimulationState state) {
		resetProcessedAgentIds();
		int timeStep = state.getStep();

		Collection<Pedestrian> peds = state.getTopography().getPedestrianDynamicElements().getElements();

		for (Pedestrian ped : peds){

			if (!getProcessedAgentIds().contains(ped.getId())) {
				LinkedList<Pedestrian> groupMembers = ped.getPedGroupMembers();
				int commandId = ped.getMostImportantStimulus().getId();

				//if (commandId > 0) {
					this.putValue(new TimestepPedestrianIdKey(timeStep, ped.getId()), commandId);

					for (Pedestrian groupMember : groupMembers) {
						// assign command id = 0 to pedestrian that follow other pedestrians (group member decisions)
						this.putValue(new TimestepPedestrianIdKey(timeStep, groupMember.getId()), 0);
						this.getProcessedAgentIds().add(groupMember.getId());
					}
				//}
			}
		}


	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
	}

	public String[] toStrings(TimestepPedestrianIdKey key){
		Integer i = this.getValue(key);
		if (i == null) {
			logger.warn(String.format("PedestrianGroupSizeProcessor does not have Data for Key: %s",
					key.toString()));
			i = -1;
		}

		return new String[]{Integer.toString(i)};
	}

	public LinkedList<Integer> getProcessedAgentIds() {
		return processedAgentIds;
	}

	public void resetProcessedAgentIds() {
		this.processedAgentIds = new LinkedList<>();
	}

	@Override
	public CompoundObject provide(CompoundObjectBuilder builder) {
		int timestep = this.getLastKey().getTimestep();
		List<TimestepPedestrianIdKey> keys = this.getKeys().stream().filter(key -> key.getTimestep() == timestep).collect(Collectors.toList());

		List<String> pedIds = new ArrayList<>();
		List<String> commandIds = new ArrayList<>();

		for (TimestepPedestrianIdKey key : keys){

			Integer commandId = this.getData().get(key);
			Integer pedId = key.getPedestrianId();

			commandIds.add(commandId.toString());
			pedIds.add(pedId.toString());
		}

		return builder.rest()
				.add(TraCIDataType.INTEGER) // timestep
				.add(TraCIDataType.STRING_LIST) //  pedestrian Id
				.add(TraCIDataType.STRING_LIST) // command Id
				.build(timestep, pedIds, commandIds);
	}

}
