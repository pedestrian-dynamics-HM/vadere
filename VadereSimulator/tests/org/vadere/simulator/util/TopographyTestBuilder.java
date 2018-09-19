package org.vadere.simulator.util;

import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.scenario.Source;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

public class TopographyTestBuilder {

	Topography topography;
	ScenarioElement lastAddedElement;

	public TopographyTestBuilder(){
		topography = new Topography();
	}

	public Topography build(){
		Topography ret = topography;
		topography = new Topography();
		return  ret;
	}

	public ScenarioElement getLastAddedElement(){
		return lastAddedElement;
	}


	TopographyTestBuilder addSource(){
		addSource(-1);
		return  this;
	}

	TopographyTestBuilder addSource(int id){
		addSource(id, new VRectangle(0,0, 10, 10));
		return  this;
	}

	TopographyTestBuilder addSource(int id, VShape s){
		addSource(new AttributesSource(id,s));
		return  this;
	}

	TopographyTestBuilder addSource(AttributesSource attr){
		Source source = new Source(attr);
		lastAddedElement = source;
		topography.addSource(source);
		return  this;
	}

	TopographyTestBuilder addTarget(){
		addSource(-1);
		return this;
	}

	TopographyTestBuilder addTarget(int id){
		addTarget(id, new VRectangle(0,0,10,10));
		return this;
	}

	TopographyTestBuilder addTarget(int id, VShape s){
		addTarget(new AttributesTarget(s, id, true));
		return this;
	}

	TopographyTestBuilder addTarget(AttributesTarget attr){
		Target target = new Target(attr);
		lastAddedElement = target;
		topography.addTarget(target);
		return this;
	}

	TopographyTestBuilder addObstacle(){
		addObstacle(-1);
		return this;
	}

	TopographyTestBuilder addObstacle(int id){
		addObstacle(id, new VRectangle(0,0,10,10));
		return this;
	}

	TopographyTestBuilder addObstacle(int id, VShape s){
		addObstacle(new AttributesObstacle(id, s));
		return this;
	}

	TopographyTestBuilder addObstacle(AttributesObstacle attr){
		Obstacle obstacle = new Obstacle(attr);
		lastAddedElement = obstacle;
		topography.addObstacle(obstacle);
		return this;
	}

}
