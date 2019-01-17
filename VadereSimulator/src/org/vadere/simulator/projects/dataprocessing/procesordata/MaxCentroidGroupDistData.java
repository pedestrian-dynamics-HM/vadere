package org.vadere.simulator.projects.dataprocessing.procesordata;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.simulator.models.groups.cgm.CentroidGroup;
import org.vadere.state.scenario.Pedestrian;

import java.util.Objects;

public class MaxCentroidGroupDistData {

	private int pedId;
	private double dist;

	public MaxCentroidGroupDistData(Pedestrian ped, CentroidGroup group){
		Pair<Pedestrian, Double> maxDist = group.getMaxDistPedIdInGroup(ped);
		this.pedId = maxDist.getKey().getId();
		this.dist = maxDist.getValue();
	}

	public MaxCentroidGroupDistData(int pedId, double dist) {
		this.pedId = pedId;
		this.dist = dist;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MaxCentroidGroupDistData that = (MaxCentroidGroupDistData) o;
		return pedId == that.pedId &&
				Double.compare(that.dist, dist) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(pedId, dist);
	}

	@Override
	public String toString() {
		return "MaxGroupDistData{" +
				"pedId=" + pedId +
				", dist=" + dist +
				'}';
	}

	public String[] toStrings(){
		String[] data = new String[]{Integer.toString(pedId), Double.toString(dist)};
		return data;
	}
}
