package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.models.groups.cgm.CentroidGroupModel;
import org.vadere.simulator.projects.dataprocessing.datakey.TimestepGroupPairKey;
import org.vadere.simulator.projects.dataprocessing.processor.util.ModelFilter;

@DataProcessorClass()
public class GroupMemberEuclideanDist extends DataProcessor<TimestepGroupPairKey, Double> implements ModelFilter {

	public GroupMemberEuclideanDist() {
		super("euclidean_dist");
	}

	@Override
	protected void doUpdate(SimulationState state) {
		int timestep = state.getStep();
		getModel(state, CentroidGroupModel.class).ifPresent(m -> {
			CentroidGroupModel model = (CentroidGroupModel)m;
			model.getGroupsById().forEach((gId, group) -> {
				group.getEuclidDist().forEach(data -> this.putValue(new TimestepGroupPairKey(timestep, gId, data.getKey()), data.getValue()));
			});
		});
	}

	@Override
	public String[] toStrings(TimestepGroupPairKey key) {
		Double val = getValue(key);
		String valStr = (val != null) ? Double.toString(val) : Double.toString(Double.NaN);
		return new String[]{valStr};
	}
}
