package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.models.groups.cgm.CentroidGroupModel;
import org.vadere.simulator.projects.dataprocessing.datakey.EventtimePedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.processor.util.ModelFilter;
import org.vadere.util.logging.Logger;

@DataProcessorClass
public class FootStepGroupIDProcessor extends DataProcessor<EventtimePedestrianIdKey, Integer> implements ModelFilter {

	private static Logger logger = Logger.getLogger(PedestrianGroupIDProcessor.class);

	public FootStepGroupIDProcessor(){
		super("groupId");
	}

	@Override
	protected void doUpdate(SimulationState state) {
		getModel(state, CentroidGroupModel.class).ifPresent(m -> { // find CentroidGroupModel
			CentroidGroupModel model = (CentroidGroupModel)m;
			model.getGroupsById().forEach((gId, group) -> {	// for each group
				group.getMembers().forEach(ped -> {			// for each member in group
					ped.getTrajectory().getFootSteps().forEach(fs -> {
						this.putValue(new EventtimePedestrianIdKey(fs.getStartTime(), ped.getId()), gId);
					});
				});
			});
		});
	}

	public String[] toStrings(EventtimePedestrianIdKey key){
		Integer i = this.getValue(key);
		if (i == null) {
			logger.warn(String.format("FootstepGroupIDProcessor does not have Data for Key: %s",
					key.toString()));
			i = -1;
		}

		return new String[]{Integer.toString(i)};
	}
}
