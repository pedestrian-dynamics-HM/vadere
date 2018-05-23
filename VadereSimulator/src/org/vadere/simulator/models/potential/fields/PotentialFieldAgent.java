package org.vadere.simulator.models.potential.fields;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.gnm.PotentialFieldPedestrianGNM;
import org.vadere.simulator.models.potential.PedestrianRepulsionPotentialCycle;
import org.vadere.simulator.models.potential.PotentialFieldPedestrianCompact;
import org.vadere.simulator.models.potential.PotentialFieldPedestrianCompactSoftshell;
import org.vadere.simulator.models.potential.PotentialFieldPedestrianOSM;
import org.vadere.simulator.models.sfm.PotentialFieldPedestrianSFM;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesPotentialCompact;
import org.vadere.state.attributes.models.AttributesPotentialCompactSoftshell;
import org.vadere.state.attributes.models.AttributesPotentialGNM;
import org.vadere.state.attributes.models.AttributesPotentialOSM;
import org.vadere.state.attributes.models.AttributesPotentialSFM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.reflection.DynamicClassInstantiator;
import org.vadere.util.reflection.VadereClassNotFoundException;

public interface PotentialFieldAgent extends Model {

	Logger logger = LogManager.getLogger(PotentialFieldAgent.class);

	@Override
	default void preLoop(double simTimeInSec) {}

	@Override
	default void postLoop(double simTimeInSec) {}

	@Override
	default void update(double simTimeInSec) {}

	/**
	 * Computes the pedestrians possessing a potential that reaches into the
	 * given relevantArea.
	 * 
	 * @param relevantArea
	 * @param pedestrian
	 *        the pedestrian in the center of the relevant area. It can be
	 *        used to determine if some pedestrians have special relation
	 *        (like group member) that would change the potential value.
	 * @param topography the current topography to enable the
	 *        {@link PotentialFieldAgent} to search for the relevant
	 *        pedestrians.
	 * @return
	 */
	Collection<? extends Agent> getRelevantAgents(VCircle relevantArea,
			Agent pedestrian, Topography topography);

	double getAgentPotential(VPoint pos, Agent pedestrian,
			Agent otherPedestrian);

	double getAgentPotential(VPoint pos, Agent pedestrian,
			Collection<? extends Agent> otherAgents);

	Vector2D getAgentPotentialGradient(VPoint pos,
			Vector2D velocity, Agent pedestrian,
			Collection<? extends Agent> otherAgents);

	static PotentialFieldAgent createPotentialField(
			final List<Attributes> modelAttributesList,
			final Topography topography,
			final AttributesAgent attributesPedestrian,
			final Random random,
			final String className) {

		DynamicClassInstantiator<PotentialFieldAgent> instantiator = new DynamicClassInstantiator<>();
		PotentialFieldAgent result = instantiator.createObject(className);

		// if the scenario has a teleporter, the cycle potential has to be added, too
		if (topography.hasTeleporter()) {
			result = new PedestrianRepulsionPotentialCycle(result, topography);
		}
		result.initialize(modelAttributesList, topography, attributesPedestrian, random);
		return result;
	}
	
}
