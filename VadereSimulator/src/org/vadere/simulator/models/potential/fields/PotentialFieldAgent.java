package org.vadere.simulator.models.potential.fields;

import java.util.Collection;
import java.util.List;

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
	default void preLoop(double simTimeInSec) {
	}

	@Override
	default void postLoop(double simTimeInSec) {
	}

	@Override
	default void update(double simTimeInSec) {
	}

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
	public Collection<? extends Agent> getRelevantAgents(VCircle relevantArea,
			Agent pedestrian, Topography topography);

	public double getAgentPotential(VPoint pos, Agent pedestrian,
			Agent otherPedestrian);

	public double getAgentPotential(VPoint pos, Agent pedestrian,
			Collection<? extends Agent> otherAgents);

	public Vector2D getAgentPotentialGradient(VPoint pos,
			Vector2D velocity, Agent pedestrian,
			Collection<? extends Agent> otherAgents);

	public static PotentialFieldAgent createPotentialField(List<Attributes> modelAttributesList,
			Topography topography, String className) {

		DynamicClassInstantiator<PotentialFieldAgent> instantiator = new DynamicClassInstantiator<>();
		Class<? extends PotentialFieldAgent> type = instantiator.getClassFromName(className);

		PotentialFieldAgent result;

		if (type == PotentialFieldPedestrianOSM.class) {
			AttributesPotentialOSM attributesPotentialOSM =
					Model.findAttributes(modelAttributesList, AttributesPotentialOSM.class);
			result = new PotentialFieldPedestrianOSM(attributesPotentialOSM);
		} else if (type == PotentialFieldPedestrianGNM.class) {
			AttributesPotentialGNM attributesPotentialGNM =
					Model.findAttributes(modelAttributesList, AttributesPotentialGNM.class);
			result = new PotentialFieldPedestrianGNM(attributesPotentialGNM);
		} else if (type == PotentialFieldPedestrianSFM.class) {
			AttributesPotentialSFM attributesPotentialSFM =
					Model.findAttributes(modelAttributesList, AttributesPotentialSFM.class);
			result = new PotentialFieldPedestrianSFM(attributesPotentialSFM);
		} else if (type == PotentialFieldPedestrianCompact.class) {
			AttributesPotentialCompact attributesPotentialCompact =
					Model.findAttributes(modelAttributesList, AttributesPotentialCompact.class);
			result = new PotentialFieldPedestrianCompact(attributesPotentialCompact);
		} else if (type == PotentialFieldPedestrianCompactSoftshell.class) {
			AttributesPotentialCompactSoftshell attributesPotentialCompactSoftshell =
					Model.findAttributes(modelAttributesList, AttributesPotentialCompactSoftshell.class);
			result = new PotentialFieldPedestrianCompactSoftshell(attributesPotentialCompactSoftshell);
		} else {
			logger.error("could not found type = " + type);
			throw new VadereClassNotFoundException();
		}

		// if the scenario has a teleporter, the cycle potential has to be added, too
		if (topography.hasTeleporter()) {
			result = new PedestrianRepulsionPotentialCycle(result, topography);
		}

		return result;
	}
	
}
