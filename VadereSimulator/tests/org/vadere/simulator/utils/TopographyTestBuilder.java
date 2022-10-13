package org.vadere.simulator.utils;

import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.attributes.scenario.AttributesStairs;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Stairs;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import java.util.Random;

public class TopographyTestBuilder {

	Topography topography;
	ScenarioElement lastAddedElement;
	Random rnd;

	public TopographyTestBuilder() {
		topography = new Topography();
		rnd = new Random(1);
	}

	public Topography build() {
		Topography ret = topography;
		topography = new Topography();
		return ret;
	}

	public ScenarioElement getLastAddedElement() {
		return lastAddedElement;
	}


	public TopographyTestBuilder addSource() {
		addSource(-1);
		return this;
	}

	public TopographyTestBuilder addSource(int id) {
		addSource(id, new VRectangle(0, 0, 10, 10));
		return this;
	}

	public TopographyTestBuilder addSource(int id, VShape s) {
		addSource(new AttributesSource(id, s));
		return this;
	}

	public TopographyTestBuilder addSource(AttributesSource attr) {
		Source source = new Source(attr);
		lastAddedElement = source;
		topography.addSource(source);
		return this;
	}

	public TopographyTestBuilder addTarget() {
		addSource(-1);
		return this;
	}

	public TopographyTestBuilder addTarget(int id) {
		addTarget(id, new VRectangle(0, 0, 10, 10));
		return this;
	}

	public TopographyTestBuilder addTarget(int id, VShape s) {
		var attribs = new AttributesTarget(s, id);
		attribs.setAbsorbing(true);
		addTarget(new AttributesTarget(s, id, true));
		return this;
	}

	public TopographyTestBuilder addTarget(AttributesTarget attr) {
		Target target = new Target(attr);
		lastAddedElement = target;
		topography.addTarget(target);
		return this;
	}

	public TopographyTestBuilder addObstacle() {
		addObstacle(-1);
		return this;
	}

	public TopographyTestBuilder addObstacle(int id) {
		addObstacle(id, new VRectangle(0, 0, 10, 10));
		return this;
	}

	public TopographyTestBuilder addObstacle(int id, VShape s) {
		addObstacle(new AttributesObstacle(id, s));
		return this;
	}

	public TopographyTestBuilder addObstacle(AttributesObstacle attr) {
		Obstacle obstacle = new Obstacle(attr);
		lastAddedElement = obstacle;
		topography.addObstacle(obstacle);
		return this;
	}

	public TopographyTestBuilder addStairs() {
		addStairs(new AttributesStairs());
		return this;
	}

	public TopographyTestBuilder addStairs(AttributesStairs attr) {
		Stairs stairs = new Stairs(attr);
		lastAddedElement = stairs;
		topography.addStairs(stairs);
		return this;
	}

	public TopographyTestBuilder addPedestrian() {

		return this;
	}

	public TopographyTestBuilder addPedestrian(AttributesAgent attr) {
		Pedestrian pedestrian = new Pedestrian(attr, rnd);
		lastAddedElement = pedestrian;
		topography.addInitialElement(pedestrian);
		return this;
	}

}
