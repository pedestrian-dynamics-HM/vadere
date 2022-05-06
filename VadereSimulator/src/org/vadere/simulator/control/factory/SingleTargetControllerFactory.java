package org.vadere.simulator.control.factory;

import org.vadere.simulator.control.scenarioelements.SingleTargetController;
import org.vadere.simulator.control.scenarioelements.TargetController;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;

public class SingleTargetControllerFactory extends TargetControllerFactory {

    @Override
    public TargetController create(Topography topography, Target target) {
        return new SingleTargetController(topography, target);
    }
}
