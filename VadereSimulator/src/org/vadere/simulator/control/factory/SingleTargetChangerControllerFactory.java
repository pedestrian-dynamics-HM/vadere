package org.vadere.simulator.control.factory;

import org.vadere.simulator.control.scenarioelements.SingleTargetChangerController;
import org.vadere.simulator.control.scenarioelements.TargetChangerController;
import org.vadere.state.scenario.TargetChanger;
import org.vadere.state.scenario.Topography;

import java.util.Random;

public class SingleTargetChangerControllerFactory extends TargetChangerControllerFactory {

    @Override
    public TargetChangerController create(Topography topography, TargetChanger targetChanger, Random random) {
        return new SingleTargetChangerController(topography, targetChanger, random);
    }
}
