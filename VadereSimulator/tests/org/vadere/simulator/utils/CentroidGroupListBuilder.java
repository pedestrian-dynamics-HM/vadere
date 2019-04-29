package org.vadere.simulator.utils;

import org.vadere.simulator.models.groups.cgm.CentroidGroup;
import org.vadere.simulator.models.groups.cgm.CentroidGroupModel;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.fail;


public class CentroidGroupListBuilder  {

	private List<CentroidGroup> out;
	private PedestrianListBuilder pedestrianListBuilder;
	private int nextPedId;
	private int nextGroupId;
	private CentroidGroupModel model;

	public CentroidGroupListBuilder(){
		out = new ArrayList<>();
		nextPedId = 1;
		nextGroupId = 1;
		pedestrianListBuilder = new PedestrianListBuilder();
		model = new CentroidGroupModel();
	}

	public CentroidGroupListBuilder setNextGroupId(int nextGroupId){
		this.nextGroupId = nextGroupId;
		return  this;
	}

	public CentroidGroupListBuilder setNextPedestrianId(int nextPedId){
		this.nextPedId = nextPedId;
		return this;
	}

	public CentroidGroupListBuilder setGroupModel(CentroidGroupModel model){
		this.model = model;
		return this;
	}

	public List<CentroidGroup> getList() {
		return out;
	}

	public CentroidGroupListBuilder clear() {
		out = new ArrayList<>();
		return this;
	}



	public CentroidGroupListBuilder add(int groupSize){
		for (int i = 0; i < groupSize; i++) {
			pedestrianListBuilder.add(nextPedId);
			nextPedId++;
		}
		out.add(makeGroup());
		return this;
	}

	public CentroidGroupListBuilder add(VPoint... points){
		for (VPoint point : points) {
			pedestrianListBuilder.add(nextPedId, point);
			nextPedId++;
		}
		CentroidGroup g = makeGroup();

		out.add(g);
		return  this;
	}

	private CentroidGroup makeGroup(){
		List<Pedestrian> peds = pedestrianListBuilder.getList();
		CentroidGroup g = new CentroidGroup(nextGroupId, peds.size(), model);
		nextGroupId++;
		peds.forEach(g::addMember);
		pedestrianListBuilder.clear();
		return g;
	}
}
