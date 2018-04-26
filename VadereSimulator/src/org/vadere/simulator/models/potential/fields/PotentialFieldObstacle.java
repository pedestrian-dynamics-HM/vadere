package org.vadere.simulator.models.potential.fields;

import java.util.List;
import java.util.Random;

import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.gnm.PotentialFieldObstacleGNM;
import org.vadere.simulator.models.potential.PotentialFieldObstacleCompact;
import org.vadere.simulator.models.potential.PotentialFieldObstacleCompactSoftshell;
import org.vadere.simulator.models.potential.PotentialFieldObstacleOSM;
import org.vadere.simulator.models.potential.PotentialFieldObstacleRingExperiment;
import org.vadere.simulator.models.sfm.PotentialFieldObstacleSFM;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesPotentialCompact;
import org.vadere.state.attributes.models.AttributesPotentialCompactSoftshell;
import org.vadere.state.attributes.models.AttributesPotentialGNM;
import org.vadere.state.attributes.models.AttributesPotentialOSM;
import org.vadere.state.attributes.models.AttributesPotentialRingExperiment;
import org.vadere.state.attributes.models.AttributesPotentialSFM;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.reflection.DynamicClassInstantiator;
import org.vadere.util.reflection.VadereClassNotFoundException;

public interface PotentialFieldObstacle extends Model {

	@Override
	default void preLoop(double simTimeInSec) {
	}

	@Override
	default void postLoop(double simTimeInSec) {
	}

	@Override
	default void update(double simTimeInSec) {
	}

	public double getObstaclePotential(VPoint pos, Agent pedestrian);

	public Vector2D getObstaclePotentialGradient(VPoint pos,
			Agent pedestrian);

	public PotentialFieldObstacle copy();

	public static PotentialFieldObstacle createPotentialField(List<Attributes> modelAttributesList,
			Topography topography, Random random, String className) {

		DynamicClassInstantiator<PotentialFieldObstacle> instantiator = new DynamicClassInstantiator<>();
		Class<? extends PotentialFieldObstacle> type = instantiator.getClassFromName(className);

		PotentialFieldObstacle result;

		if (type == PotentialFieldObstacleOSM.class) {
			AttributesPotentialOSM attributesPotentialOSM =
					Model.findAttributes(modelAttributesList, AttributesPotentialOSM.class);
			result = new PotentialFieldObstacleOSM(attributesPotentialOSM, topography.getObstacles());
		} else if (type == PotentialFieldObstacleGNM.class) {
			AttributesPotentialGNM attributesPotentialGNM =
					Model.findAttributes(modelAttributesList, AttributesPotentialGNM.class);
			result = new PotentialFieldObstacleGNM(topography.getObstacles(), attributesPotentialGNM);
		} else if (type == PotentialFieldObstacleSFM.class) {
			AttributesPotentialSFM attributesPotentialSFM =
					Model.findAttributes(modelAttributesList, AttributesPotentialSFM.class);
			result = new PotentialFieldObstacleSFM(topography.getObstacles(), attributesPotentialSFM);
		} else if (type == PotentialFieldObstacleRingExperiment.class) {
			AttributesPotentialRingExperiment attributesPotentialRingExperiment =
					Model.findAttributes(modelAttributesList, AttributesPotentialRingExperiment.class);
			result = new PotentialFieldObstacleRingExperiment(attributesPotentialRingExperiment);
		} else if (type == PotentialFieldObstacleCompact.class) {
			AttributesPotentialCompact attributesPotentialCompact =
					Model.findAttributes(modelAttributesList, AttributesPotentialCompact.class);
			result = new PotentialFieldObstacleCompact(attributesPotentialCompact, topography.getObstacles(), random);
		} else if (type == PotentialFieldObstacleCompactSoftshell.class) {
			AttributesPotentialCompactSoftshell attributesPotentialCompactSoftshell =
					Model.findAttributes(modelAttributesList, AttributesPotentialCompactSoftshell.class);
			result = new PotentialFieldObstacleCompactSoftshell(attributesPotentialCompactSoftshell,
					topography, random);
		} else {
			throw new VadereClassNotFoundException();
		}

		return result;
	}
}
