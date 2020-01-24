package org.vadere.simulator.models.potential.fields;

import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.queuing.PotentialFieldTargetQueuingGrid;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.models.AttributesPotentialRingExperiment;
import org.vadere.state.attributes.models.AttributesQueuingGame;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.reflection.DynamicClassInstantiator;
import org.vadere.util.reflection.VadereClassNotFoundException;

import java.util.List;
import java.util.Map;

/**
 * A static (needsUpdate returns always false) or dynamic target potential field which offers
 * a {@link CellGrid} sampling.
 *
 * @author Benedikt Zoennchen
 */
public interface IPotentialFieldTargetGrid extends IPotentialFieldTarget {

    /**
     * Returns a Map: targetId -> CellGrid (reference). The CellGrid (a Cartesian grid) contains the current values of the potential field
     * for its grid points. For performance reason only a reference, i.e. no deep copy, of CellGrids are returned. If the potential field
     * is dynamic these values will change during the simulation run!
     *
     * @return  a Map: targetId -> CellGrid (reference)
     */
	Map<Integer, CellGrid> getCellGrids();

    /**
     * A factory method to create different target potential fields which use a Cartesian grid.
     *
     * @param modelAttributesList   list of model attributes (models pick their attributes themselves)
     * @param domain                the spatial domain of the scenario
     * @param attributesPedestrian  the attributes of pedestrians
     * @param className             the name of the class of the field which will be created
	 * @return target potential fields which use a Cartesian grid
     */
	static IPotentialFieldTargetGrid createPotentialField(final List<Attributes> modelAttributesList,
                                                          final Domain domain,
                                                          final AttributesAgent attributesPedestrian,
														  String className) {

		DynamicClassInstantiator<IPotentialFieldTarget> instantiator = new DynamicClassInstantiator<>();

		Class<? extends IPotentialFieldTarget> type = instantiator.getClassFromName(className);

        IPotentialFieldTargetGrid result;

		if (type == PotentialFieldTargetGrid.class) {
			AttributesFloorField attributesFloorField =
					Model.findAttributes(modelAttributesList, AttributesFloorField.class);
			result = new PotentialFieldTargetGrid(domain, attributesPedestrian, attributesFloorField);
		} else if (type == PotentialFieldTargetQueuingGrid.class) {
			AttributesQueuingGame attributesQueuingGame =
					Model.findAttributes(modelAttributesList, AttributesQueuingGame.class);
			result = new PotentialFieldTargetQueuingGrid(domain, attributesPedestrian, attributesQueuingGame);
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
