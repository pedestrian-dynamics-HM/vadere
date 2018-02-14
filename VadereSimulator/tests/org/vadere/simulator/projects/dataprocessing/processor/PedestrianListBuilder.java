package org.vadere.simulator.projects.dataprocessing.processor;

import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class PedestrianListBuilder {

	private List<Pedestrian> out;
	private Pedestrian p;

	public PedestrianListBuilder() {
		this.out = new LinkedList<>();
	}

	public PedestrianListBuilder add(int id) {
		out.add(new Pedestrian(new AttributesAgent(id), new Random()));
		return this;
	}

	public PedestrianListBuilder add(int id, VPoint pos) {
		Pedestrian p = new Pedestrian(new AttributesAgent(id), new Random());
		p.setPosition(pos);
		out.add(p);
		return this;
	}

	public PedestrianListBuilder add(Integer... ids) {
		for (Integer id : ids) {
			Pedestrian p = new Pedestrian(new AttributesAgent(id), new Random());
			out.add(p);
		}
		return this;
	}

	public PedestrianListBuilder clear() {
		out = new LinkedList<>();
		return this;
	}

	public List<Pedestrian> getList() {
		return out;
	}

}
