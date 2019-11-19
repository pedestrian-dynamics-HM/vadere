package org.vadere.simulator.projects.dataprocessing.processor;


import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.models.groups.cgm.CentroidGroupModel;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepPedestrianIdKey;
import org.vadere.simulator.projects.dataprocessing.procesordata.MaxCentroidGroupDistData;
import org.vadere.simulator.projects.dataprocessing.processor.util.ModelFilter;


/**
 *
 */
@DataProcessorClass
public class PedestrianGroupMaxDistProcessor extends  DataProcessor<TimestepPedestrianIdKey, MaxCentroidGroupDistData> implements ModelFilter {

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
