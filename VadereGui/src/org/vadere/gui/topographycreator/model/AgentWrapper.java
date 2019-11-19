package org.vadere.gui.topographycreator.model;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import java.util.LinkedList;
import java.util.Random;

/**
 * The AgentWrapper wraps an AgentInitialStore to a ScenarioElement, so
 * the TopographyCreator can treat every Element the same way. In the Creator
 * every Element can be drawn to the screen so every Element contains an VShape
 * especially the PedestrianInitialStore.
 *
 *
 */
public final class AgentWrapper extends ScenarioElement {

	/** The wrapped store object. */
	private Agent agent;

	AgentWrapper(final VPoint position) {
		// use a Pedestrian as default
		// TODO this default does not make much sense
		this.agent = new Pedestrian(new AttributesAgent(), new Random());
		this.agent.setPosition(position);
		this.agent.setTargets(new LinkedList<Integer>());
	}

	public AgentWrapper(final Agent agent) {
		this.agent = (Agent) agent.clone();
	}

	public Agent getAgentInitialStore() {
		return agent;
	}

	public void setAgentInitialStore(final Agent store) {
		this.agent = store;
	}
	
	@Override
	public void setShape(VShape newShape) {
		agent.setShape(newShape);
	}

	@Override
	public VShape getShape() {
		return agent.getShape();
	}

	@Override
	public int getId() {
		return agent.getId();
	}

	@Override
	public void setId(int id) {
		agent.setId(id);
	}

	@Override
	public ScenarioElementType getType() {
		// TODO bug - this is a agent wrapper, not necessarily an pedestrian wrapper
		return ScenarioElementType.PEDESTRIAN;
	}

	@Override
	public Attributes getAttributes() {
		return agent.getAttributes();
	}

	@Override
	public void setAttributes(Attributes attributes) {
		this.agent.setAttributes(attributes);
	}

	@Override
	public AgentWrapper clone() {
		return new AgentWrapper((Agent) agent.clone());
	}

}
