package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.models.groups.cgm.CentroidGroupModel;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.util.logging.Logger;
import org.vadere.simulator.projects.dataprocessing.processor.util.ModelFilter;

@DataProcessorClass
public class PedestrianGroupIDProcessor extends DataProcessor<TimestepPedestrianIdKey, Integer> implements ModelFilter {

	private static Logger logger = Logger.getLogger(PedestrianGroupIDProcessor.class);

	public PedestrianGroupIDProcessor(){
		super("groupId");
	}

	@Override
	protected void doUpdate(SimulationState state) {
		Integer timeStep = state.getStep();

		getModel(state, CentroidGroupModel.class).ifPresent(m -> { // find CentroidGroupModel
			CentroidGroupModel model = (CentroidGroupModel)m;
			model.getGroupsById().forEach((gId, group) -> {	// for each group
				group.getMembers().forEach(ped -> {			// for each member in group
					this.putValue(new TimestepPedestrianIdKey(timeStep, ped.getId()), gId);
				});
			});
		});
	}

	@Override
	public void init(final ProcessorManager manager) {
		super.init(manager);
	}

	public String[] toStrings(TimestepPedestrianIdKey key){
		Integer i = this.getValue(key);
		if (i == null) {
			logger.warn(String.format("PedestrianGroupIDProcessor does not have Data for Key: %s",
					key.toString()));
			i = -1;
		}

		return new String[]{Integer.toString(i)};
	}
}
