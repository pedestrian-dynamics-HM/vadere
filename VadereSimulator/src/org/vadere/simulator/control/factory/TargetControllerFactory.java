package org.vadere.simulator.control.factory;

import org.vadere.simulator.control.scenarioelements.TargetController;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;

public abstract class TargetControllerFactory {
        abstract public TargetController create(Topography topography, Target target);
}
