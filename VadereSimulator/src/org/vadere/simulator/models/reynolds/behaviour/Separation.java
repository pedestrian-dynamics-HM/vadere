package org.vadere.simulator.models.reynolds.behaviour;

import java.util.Collection;
import java.util.Iterator;

import org.vadere.simulator.models.reynolds.PedestrianReynolds;
import org.vadere.simulator.models.reynolds.ReynoldsSteeringModel;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.Vector2D;

public class Separation {

	private ReynoldsSteeringModel model;

	public Separation(ReynoldsSteeringModel model) {
		this.model = model;
	}

	public Vector2D nextStep(double simTime, Vector2D currentMov, PedestrianReynolds self) {
		double radius = self.getAttributes().getRadius();

		Collection<Pedestrian> peds = model.getScenario().getElements(Pedestrian.class);
		Iterator<Pedestrian> it = peds.iterator();
		Pedestrian p;

		Vector2D mov = new Vector2D(0, 0);
		Vector2D nextPos = new Vector2D(self.getPosition()).add(currentMov);

		// iterate over every neighbor, o(n^2)
		while (it.hasNext()) {
			p = it.next();

			// skip itself
			if (p.getId() == self.getId()) {
				continue;
			}

			// distance to the neighbor, from the future position
			Vector2D dist = new Vector2D(p.getPosition().subtract(nextPos));

			// if neighbor is too far every, ignore him
			if (dist.getLength() >= 2 * radius) {
				continue;
			}

			// calculate correction vector, to avoid collision
			dist = dist.normalize(2.1 * radius - dist.getLength()).multiply(-1);

			// add to movement
			nextPos = nextPos.add(dist);
			mov = mov.add(dist);
		}

		// This will iterate over every neighbor only once and will thus not be able to find
		// the best, available spot for the next movement. This behavior is based on the
		// assumption, that the decisions of the direction of the human movement are often
		// not perfect and if the agent does not find a good spot in a decent amount of time,
		// it will wait (skip the turn) and try again.

		// If still in collision, negate current movement and stand still
		it = peds.iterator();
		while (it.hasNext()) {
			p = it.next();
			if (p.getId() == self.getId()) {
				continue;
			}

			Vector2D dist = new Vector2D(p.getPosition().subtract(nextPos));
			if (dist.getLength() < 2 * radius) {
				return currentMov.multiply(-1);
			}
		}

		return mov;
	}

}
