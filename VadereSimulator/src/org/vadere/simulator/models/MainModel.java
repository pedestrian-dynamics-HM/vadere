package org.vadere.simulator.models;

import java.util.List;

/**
 * A main model of a simulation which can include submodels.
 * 
 */
public interface MainModel extends Model, DynamicElementFactory {

	List<Model> getActiveCallbacks();

}
