package org.vadere.simulator.models.reynolds.behaviour;

import java.util.Collection;
import java.util.Iterator;

import org.vadere.simulator.models.reynolds.PedestrianReynolds;
import org.vadere.simulator.models.reynolds.ReynoldsSteeringModel;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.Vector2D;

/**
 * Take the current movement and shorten it, if a collision would occur.
 * 
 */
public class CollisionAvoidance {

	private ReynoldsSteeringModel model;

	public CollisionAvoidance(ReynoldsSteeringModel model) {
		this.model = model;
	}

	public Vector2D nextStep(double simTime, Vector2D currentMov, PedestrianReynolds self) {
		double radius = self.getAttributes().getRadius();

		Collection<Pedestrian> peds = model.getScenario().getElements(Pedestrian.class);
		Iterator<Pedestrian> it = peds.iterator();
		Pedestrian p;

		Vector2D toNeighbor;
		Vector2D proj;
		Vector2D norm;

		// Only avoid collisions, which would occur in front of us.
		Vector2D pos = new Vector2D(self.getPosition().add(currentMov.normalize(radius)));
		Vector2D mov = currentMov.sub(currentMov.normalize(radius));

		while (it.hasNext()) {
			p = it.next();
			if (p.getId() == self.getId()) {
				continue;
			}

			// get vector from pedestrian to neighbor
			toNeighbor = new Vector2D(p.getPosition().subtract(pos));

			// skip neighbor farer away than current movement
			if (toNeighbor.getLength() > mov.getLength()) {
				continue;
			}

			// project toNeighbor to current movement
			proj = mov.multiply(
					(mov.x * toNeighbor.x + mov.y * toNeighbor.y)
							/ (Math.pow(mov.x, 2) + Math.pow(mov.y, 2)));

			// if projection has different leading sign than current movement, our neighbor is
			// behind us.
			if ((proj.x < 0 && mov.x > 0 || proj.x > 0 && mov.x < 0) ||
					(proj.y < 0 && mov.y > 0 || proj.y > 0 && mov.y < 0)) {
				continue;
			}

			// get normal from neighbor to current movement
			// if it is more than twice the pedestrian radius, the neighbor
			// is not in our way, continue
			norm = toNeighbor.sub(proj);
			if (norm.getLength() > 2 * radius) {
				continue;
			}

			// if projection is shorter than twice the ped radius, our neighbor is too
			// close, we have to skip our current movement
			if (proj.getLength() < 2 * radius) {
				return mov.multiply(-1);
			}

			// return difference between new and old movement as our correction vector
			return proj.sub(mov);
		}

		// No correction needed
		return new Vector2D(0, 0);
	}

}
