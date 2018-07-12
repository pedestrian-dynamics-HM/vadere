package org.vadere.simulator.models;

import org.vadere.simulator.control.factory.SingleSourceControllerFactory;
import org.vadere.simulator.control.factory.SourceControllerFactory;

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

}
