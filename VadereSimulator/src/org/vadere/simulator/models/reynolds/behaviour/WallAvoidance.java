package org.vadere.simulator.models.reynolds.behaviour;

import org.vadere.simulator.models.reynolds.PedestrianReynolds;
import org.vadere.simulator.models.reynolds.ReynoldsSteeringModel;
import org.vadere.util.geometry.shapes.Vector2D;

public class WallAvoidance {

	private ReynoldsSteeringModel model;

	public WallAvoidance(ReynoldsSteeringModel model) {
		this.model = model;
	}

	public Vector2D nextStep(double simTime, Vector2D currentMov, PedestrianReynolds ped) {
		return new Vector2D(0, 0);
	}

}
