package org.vadere.simulator.control.external.models;

import org.vadere.simulator.control.external.reaction.ReactionModel;
import org.vadere.state.scenario.Topography;

/**
 * This class encapsulates the creation of a concrete {@link IControlModel}
 * which is defined by the user in the crowd guidance simulator over TraCI.
 *
 * The user provides the simple class name.
 * I.e., no fully qualified classname.
 */


public interface IControlModel {


    void update(Topography topo, Double time, String commandStr, Integer pedId);
    void update(Topography topography, Double time, String command);

    void setReactionModel(ReactionModel reactionModel);
}
