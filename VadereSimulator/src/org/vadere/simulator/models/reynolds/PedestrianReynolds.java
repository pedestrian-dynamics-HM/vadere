package org.vadere.simulator.models.reynolds;

import java.util.Random;

import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;

public class PedestrianReynolds extends Pedestrian {

	private Vector2D lastMovement;
	private double startTime;

	public PedestrianReynolds(AttributesAgent attributesPedestrian, Random random) {
		super(attributesPedestrian, random);

		this.lastMovement = new Vector2D(0, 0);
		this.startTime = -1;
	}

	public VPoint getLastMovement() {
		return lastMovement;
	}

	public double getStartTime() {
		return startTime;
	}

	public void move(double simTime, Vector2D mov) {
		lastMovement = mov;

		if (startTime < 0) {
			startTime = simTime;
		}

		this.setPosition(this.getPosition().add(mov));
	}

}
