package org.vadere.gui.topographycreator.model;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.attributes.scenario.AttributesStairs;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VShape;

/**
 * A Factory to create new ScenarioElements.
 * 
 *
 */
public class TopographyElementFactory {
	private static TopographyElementFactory instance = new TopographyElementFactory();

	private TopographyElementFactory() {}

	public static TopographyElementFactory getInstance() {
		return instance;
	}

	public ScenarioElement createScenarioShape(final ScenarioElementType type, final VShape shape) {
		switch (type) {
			case OBSTACLE:
				return new org.vadere.state.scenario.Obstacle(new AttributesObstacle(-1, shape));
			case STAIRS:
				return new org.vadere.state.scenario.Stairs(new AttributesStairs(-1, shape, 1, new Vector2D(1.0, 0.0)));
			case SOURCE:
				return new org.vadere.state.scenario.Source(new AttributesSource(-1, shape));
			case TARGET:
				return new org.vadere.state.scenario.Target(new AttributesTarget(shape));
			case PEDESTRIAN:
				return new AgentWrapper(((VCircle) shape).getCenter());
			default:
				throw new IllegalArgumentException("unsupported ScenarioElementType.");
		}
	}

	public <T extends Attributes> ScenarioElement createScenarioShape(final T attributes) {
		if (attributes instanceof AttributesObstacle) {
			return new org.vadere.state.scenario.Obstacle((AttributesObstacle) attributes);
		} else if (attributes instanceof AttributesStairs) {
			return new org.vadere.state.scenario.Stairs((AttributesStairs) attributes);
		} else if (attributes instanceof AttributesSource) {
			return new org.vadere.state.scenario.Source((AttributesSource) attributes);
		} else if (attributes instanceof AttributesTarget) {
			return new org.vadere.state.scenario.Target((AttributesTarget) attributes);
		} else {
			throw new IllegalArgumentException("unsupported Attributes.");
		}
	}

	public ScenarioElement createScenarioShape(final AttributesTarget attributes) {
		return new org.vadere.state.scenario.Target(attributes);
	}

	public ScenarioElement createScenarioShape(final AttributesSource attributes) {
		return new org.vadere.state.scenario.Source(attributes);
	}
}
