package org.vadere.simulator.projects.dataprocessing.processor.util;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.groups.cgm.CentroidGroupModel;
import org.vadere.state.scenario.Topography;

import java.util.Optional;

public interface ModelFilter {

	/**
	 *
	 * @param state			SimulationState  containing model information
	 * @param modelClass	Class of the searched SubModel
	 * @return				Optional of the SubModel
	 */
	default Optional<Model> getModel (final SimulationState state, Class modelClass){
		Optional<MainModel> mainModel =  state.getMainModel();
		return mainModel.flatMap(mainModel1 -> mainModel1
				.getSubmodels().stream()
				.filter(modelClass::isInstance)
				.findAny());
	}


}
