package org.vadere.simulator.models.reynolds.behaviour;

import java.util.Iterator;
import java.util.List;

import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.simulator.models.reynolds.PedestrianReynolds;
import org.vadere.simulator.models.reynolds.ReynoldsSteeringModel;
import org.vadere.state.scenario.Obstacle;

public class Containment {

	private ReynoldsSteeringModel model;

	public Containment(ReynoldsSteeringModel model) {
		this.model = model;
	}

	public Vector2D nextStep(double simTime, Vector2D currentMov, PedestrianReynolds self) {
		double radius = self.getAttributes().getRadius();

		List<Obstacle> obstacles = model.getScenario().getObstacles();
		Iterator<Obstacle> it = obstacles.iterator();
		Obstacle o;

		Vector2D mov = new Vector2D(0, 0);
		Vector2D nextPos = new Vector2D(self.getPosition()).add(currentMov);

		// iterate over every obstacle o(n^2)
		while (it.hasNext()) {
			o = it.next();

			// if we are already inside an obstacle, try to escape by negating the current movement
			// this will also work by negating the last movement, maybe better imo
			if (o.getShape().contains(self.getPosition())) {
				return currentMov.multiply(-2);
			}

			// get distance from itself to the closest point of the obstacle
			Vector2D dist = new Vector2D(o.getShape().closestPoint(self.getPosition())).sub(self.getPosition());

			// get distance from the next position to the closest point of the obstacle
			Vector2D nextDist = new Vector2D(o.getShape().closestPoint(nextPos)).sub(nextPos);

			// if the pedestrian does not end in the obstacle with his next movement, ignore
			// obstacle
			if (!o.getShape().contains(nextPos) && nextDist.getLength() > 1.05 * radius) {
				continue;
			}

			// project movement onto distance from position to obstacle
			Vector2D proj = dist.multiply(
					(dist.x * currentMov.x + dist.y * currentMov.y)
							/ (Math.pow(dist.x, 2) + Math.pow(dist.y, 2)));

			// calculate correction vector to avoid walking into the obstacle
			if (dist.getLength() < proj.getLength()) {
				mov = dist.sub(proj);
				mov = mov.normalize(mov.getLength() + 1.05 * radius);
			} else if (dist.getLength() > proj.getLength()) {
				mov = proj.sub(dist);
				mov = mov.normalize(mov.getLength() + 1.05 * radius);
			} else {
				mov = dist.multiply(-1).normalize(1.05 * radius);
			}

			return mov;
		}

		return new Vector2D(0, 0);
	}

}
