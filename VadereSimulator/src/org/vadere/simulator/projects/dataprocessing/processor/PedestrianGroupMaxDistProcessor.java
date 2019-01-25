package org.vadere.simulator.projects.dataprocessing.processor;


import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.groups.cgm.CentroidGroupModel;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.procesordata.MaxCentroidGroupDistData;
import org.vadere.simulator.projects.dataprocessing.processor.util.ModelFilter;
import org.vadere.state.scenario.Pedestrian;

import java.util.Optional;


/**
 *
 */
@DataProcessorClass
public class PedestrianGroupMaxDistProcessor extends  DataProcessor<TimestepPedestrianIdKey, MaxCentroidGroupDistData> implements ModelFilter {

	private static Logger logger = LogManager.getLogger(PedestrianGroupIDProcessor.class);

	public PedestrianGroupMaxDistProcessor(){
		super("maxDistToGroupMember", "pedIdMaxDistToGroupMemner");
	}

	@Override
	protected void doUpdate(SimulationState state) {
		Integer timeStep = state.getStep();

		getModel(state, CentroidGroupModel.class).ifPresent(m -> { // find CentroidGroupModel
			CentroidGroupModel model = (CentroidGroupModel)m;
			model.getGroupsById().forEach((gId, group) -> {	// for each group
				group.getMembers().forEach(ped -> {			// for each member in group
					this.putValue(new TimestepPedestrianIdKey(timeStep, ped.getId()),
							new MaxCentroidGroupDistData(ped, group));
				});
			});
		});
	}

	public String[] toStrings(final TimestepPedestrianIdKey key){
		return this.hasValue(key) ? this.getValue(key).toStrings() : new String[]{"N/A", "N/A"};
	}


}
