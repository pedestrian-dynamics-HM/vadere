package org.vadere.simulator.models.potential.fields;

import java.util.HashMap;
import java.util.List;

import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.queuing.PotentialFieldTargetQueuingGrid;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.models.AttributesPotentialRingExperiment;
import org.vadere.state.attributes.models.AttributesQueuingGame;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.potential.CellGrid;
import org.vadere.util.reflection.DynamicClassInstantiator;
import org.vadere.util.reflection.VadereClassNotFoundException;

public interface IPotentialTargetGrid extends PotentialFieldTarget {
	HashMap<Integer, CellGrid> getCellGrids();

	static IPotentialTargetGrid createPotentialField(List<Attributes> modelAttributesList,
			Topography topography, AttributesAgent attributesPedestrian, String className) {

		DynamicClassInstantiator<IPotentialTargetGrid> instantiator = new DynamicClassInstantiator<>();
		Class<? extends IPotentialTargetGrid> type = instantiator.getClassFromName(className);

		IPotentialTargetGrid result;

		if (type == PotentialFieldTargetGrid.class) {
			AttributesFloorField attributesFloorField =
					Model.findAttributes(modelAttributesList, AttributesFloorField.class);
			result = new PotentialFieldTargetGrid(topography, attributesPedestrian, attributesFloorField);
		} else if (type == PotentialFieldTargetQueuingGrid.class) {
			AttributesQueuingGame attributesQueuingGame =
					Model.findAttributes(modelAttributesList, AttributesQueuingGame.class);
			result = new PotentialFieldTargetQueuingGrid(topography, attributesPedestrian, attributesQueuingGame);
		} else if (type == PotentialFieldTargetRingExperiment.class) {
			AttributesPotentialRingExperiment attributesPotentialRingExperiment =
					Model.findAttributes(modelAttributesList, AttributesPotentialRingExperiment.class);
			result = new PotentialFieldTargetRingExperiment(attributesPotentialRingExperiment);
		} else {
			throw new VadereClassNotFoundException();
		}

		return result;
	}
}
