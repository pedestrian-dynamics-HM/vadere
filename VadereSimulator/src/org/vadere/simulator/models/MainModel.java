package org.vadere.simulator.models;

import java.util.List;

import org.vadere.simulator.control.ActiveCallback;

/**
 * A main model of a simulation which can include submodels.
 * 
 */
public interface MainModel
		extends Model, ActiveCallback, DynamicElementFactory {

	List<ActiveCallback> getActiveCallbacks();

}
