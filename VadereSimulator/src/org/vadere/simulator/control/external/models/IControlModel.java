package org.vadere.simulator.control.external.models;

import org.vadere.simulator.control.external.reaction.InformationFilterSettings;
import org.vadere.simulator.control.psychology.perception.StimulusController;
import org.vadere.state.scenario.Topography;

/**
 * This class encapsulates the creation of a concrete {@link IControlModel}
 * which is defined by the user in the crowd guidance simulator over TraCI.
 *
 * The user provides the simple class name.
 * I.e., no fully qualified classname.
 */


public interface IControlModel {

    void init(final Topography topo, final StimulusController stimulusController, final double simTimeStepLength, final InformationFilterSettings informationFilterSettings);

    void update(String commandStr, Double time, int pedId);
}
