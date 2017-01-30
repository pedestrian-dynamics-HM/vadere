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

/**
 * A static (needsUpdate returns always false) or dynamic target potential field which uses a
 * Cartesian grid for discretization.
 */
public interface IPotentialFieldTargetGrid extends IPotentialFieldTarget {

    /**
     * Returns the Cartesian grid which contains the calculated values.
     * @return  the Cartesian grid
     */
	HashMap<Integer, CellGrid> getCellGrids();

    /**
     * A factory method to create different target potential fields which use a Cartesian grid.
     *
     * @param modelAttributesList   list of model attributes (models pick their attributes themselves)
     * @param topography            the topography
     * @param attributesPedestrian  the attributes of pedestrians
     * @param className             the name of the class of the field which will be created
     * @return target potential fields which use a Cartesian grid
     */
	static IPotentialFieldTargetGrid createPotentialField(final List<Attributes> modelAttributesList,
                                                          final Topography topography,
                                                          final AttributesAgent attributesPedestrian, String className) {

		DynamicClassInstantiator<IPotentialFieldTarget> instantiator = new DynamicClassInstantiator<>();

		Class<? extends IPotentialFieldTarget> type = instantiator.getClassFromName(className);

        IPotentialFieldTargetGrid result;

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
