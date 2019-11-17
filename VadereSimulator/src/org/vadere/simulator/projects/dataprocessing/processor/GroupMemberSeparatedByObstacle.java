package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.models.groups.cgm.CentroidGroupModel;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepGroupPairKey;
import org.vadere.simulator.projects.dataprocessing.processor.util.ModelFilter;

@DataProcessorClass()
public class GroupMemberSeparatedByObstacle extends DataProcessor<TimestepGroupPairKey, Boolean> implements ModelFilter {

	public GroupMemberSeparatedByObstacle(){
		super("intersect_obstacle");
	}

	@Override
	protected void doUpdate(SimulationState state) {
		int timestep = state.getStep();
		getModel(state, CentroidGroupModel.class).ifPresent(m -> {
			CentroidGroupModel model = (CentroidGroupModel)m;
			model.getGroupsById().forEach((gId, group) -> {
				group.getPairIntersectObstacle().forEach(data -> this.putValue(new TimestepGroupPairKey(timestep, gId, data.getKey()), data.getValue()));
			});
		});
	}

	@Override
	public String[] toStrings(TimestepGroupPairKey key) {
		Boolean val = getValue(key);
		String valStr = (val != null) ? Boolean.toString(val) : Boolean.toString(false);
		return new String[]{valStr};
	}
}
