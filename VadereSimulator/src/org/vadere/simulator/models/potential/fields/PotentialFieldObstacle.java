package org.vadere.simulator.models.potential.fields;

import java.util.List;
import java.util.Random;

import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.reflection.DynamicClassInstantiator;

public interface PotentialFieldObstacle extends Model {

	@Override
	default void preLoop(double simTimeInSec) {}

	@Override
	default void postLoop(double simTimeInSec) {}

	@Override
	default void update(double simTimeInSec) {}

	double getObstaclePotential(IPoint pos, Agent pedestrian);

	Vector2D getObstaclePotentialGradient(VPoint pos, Agent pedestrian);

	PotentialFieldObstacle copy();

	static PotentialFieldObstacle createPotentialField(
			final List<Attributes> modelAttributesList,
			final Domain domain,
			final AttributesAgent attributesPedestrian,
			final Random random,
			final String className) {

		DynamicClassInstantiator<PotentialFieldObstacle> instantiator = new DynamicClassInstantiator<>();
		PotentialFieldObstacle result = instantiator.createObject(className);
		result.initialize(modelAttributesList, domain, attributesPedestrian, random);
		return result;
	}
}
