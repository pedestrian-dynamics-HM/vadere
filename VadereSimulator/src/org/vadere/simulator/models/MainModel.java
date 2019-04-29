package org.vadere.simulator.models;

import org.vadere.simulator.control.factory.SingleSourceControllerFactory;
import org.vadere.simulator.control.factory.SourceControllerFactory;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Topography;

import java.util.List;

/**
 * A main model of a simulation which can include submodels.
 * 
 */
public interface MainModel extends Model, DynamicElementFactory {

	List<Model> getSubmodels();

	default SourceControllerFactory getSourceControllerFactory() {
		return new SingleSourceControllerFactory();
	}

	@Override
	default int registerDynamicElementId(final Topography topography, int id) {
		int pedId;
		if (id == AttributesAgent.ID_NOT_SET){
			pedId = topography.getNextDynamicElementId();
		} else {
			pedId = topography.getNextDynamicElementId(id);
		}
		return pedId;
	}

	@Override
	default int getNewDynamicElementId(final Topography topography) {
		return registerDynamicElementId(topography, AttributesAgent.ID_NOT_SET);
	}
}
