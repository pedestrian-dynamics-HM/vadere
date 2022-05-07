package org.vadere.simulator.control.factory;

import org.vadere.simulator.control.scenarioelements.TargetChangerController;
import org.vadere.state.scenario.TargetChanger;
import org.vadere.state.scenario.Topography;

import java.util.Random;

public abstract class TargetChangerControllerFactory {
        abstract public TargetChangerController create(Topography topography, TargetChanger targetChanger, Random random);
}
