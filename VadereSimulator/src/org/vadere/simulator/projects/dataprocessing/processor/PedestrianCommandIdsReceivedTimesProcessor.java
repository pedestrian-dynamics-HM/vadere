package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.logging.Logger;

import java.util.Collection;
import java.util.LinkedList;

@DataProcessorClass
public class PedestrianCommandIdsReceivedTimesProcessor extends DataProcessor<TimestepPedestrianIdKey, Integer> {

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
}
