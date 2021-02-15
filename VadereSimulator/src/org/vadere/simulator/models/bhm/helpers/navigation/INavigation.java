package org.vadere.simulator.models.bhm.helpers.navigation;

import org.vadere.simulator.models.bhm.PedestrianBHM;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.Random;

public interface INavigation {
	public void initialize(PedestrianBHM pedestrianBHM, Topography topography, Random random);
	public VPoint getNavigationPosition();
}
