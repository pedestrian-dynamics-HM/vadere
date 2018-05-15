package org.vadere.simulator.models.potential;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesPotentialOSM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VPoint;

@ModelClass
public class PotentialFieldObstacleOSM implements PotentialFieldObstacle {

	private final AttributesPotentialOSM attributes;
	private Collection<Obstacle> obstacles;

	public PotentialFieldObstacleOSM(
			AttributesPotentialOSM attributesDynamicPotentialOSM,
			Collection<Obstacle> obstacles) {
		this.attributes = attributesDynamicPotentialOSM;
		this.obstacles = obstacles;
	}

	@Override
	public double getObstaclePotential(VPoint pos, Agent pedestrian) {

		double potential = 0;
		double repulsion = 0;
		for (Obstacle obstacle : obstacles) {

			// Shapes of pedestrians are assumed to be circles.
			double distance = obstacle.getShape().distance(pos)
					- pedestrian.getRadius();

			if (distance <= 0) {
				repulsion = attributes.getObstacleBodyPotential();
			} else if (distance < attributes.getObstacleRepulsionWidth()) {

				// See [seitz and koester, 2012], formula (2)
				repulsion = Math.exp(-Math.pow(distance,
						attributes.getBObsOSM())
						* attributes.getAObsOSM())
						* attributes.getObstacleRepulsionStrength();
			}

			// TODO [priority=medium] [task=bugfix] [Error?]: shouldn't the potential of all obstacles be added instead of taking the maximum? see paper 'Natural discretization...'
			// TODO [priority=medium] [task=feature] If this algorithm is chosen: first compute distance, choose closest, and then compute potential.
			if (repulsion > potential) {
				potential = repulsion;
			}
		}

		return potential;
	}

	@Override
	public Vector2D getObstaclePotentialGradient(VPoint pos,
			Agent pedestrian) {

		Vector2D result;

		Obstacle closestObstacle = null;
		double closestDistance = Double.POSITIVE_INFINITY;

		for (Obstacle obstacle : obstacles) {

			// Shapes of pedestrians are assumed to be circles.
			double distance = obstacle.getShape().distance(pos)
					- pedestrian.getRadius();

			if (closestDistance > distance) {
				closestObstacle = obstacle;
				closestDistance = distance;
			}
		}

		if (closestObstacle != null) {
			result = getObstaclePotentialGradient(pos, closestObstacle,
					pedestrian, closestDistance);
		} else {
			result = new Vector2D(0, 0);
		}

		return result;
	}

	private Vector2D getObstaclePotentialGradient(VPoint pos, Obstacle obs,
			Agent pedestrian, double distance) {

		Vector2D result;

		if (distance >= 0 && distance < attributes.getObstacleRepulsionWidth()) {

			VPoint closestPoint = obs.getShape().closestPoint(pos);
			Vector2D direction = new Vector2D(pos.x - closestPoint.x, pos.y
					- closestPoint.y);
			direction = direction.normalize(distance);

			// Part of the gradient that is the same for both vx and vy.
			double vu = -attributes.getAObsOSM()
					* attributes.getBObsOSM()
					* Math.pow(distance, attributes.getBObsOSM() / 2.0 - 1.0)
					* Math.exp(-attributes.getAObsOSM()
							* Math.pow(distance, attributes.getBObsOSM() / 2.0))
					* attributes.getObstacleRepulsionStrength();

			result = new Vector2D(vu * direction.x, vu * direction.y);
		} else {
			result = new Vector2D(0, 0);
		}

		return result;
	}

	@Override
	public PotentialFieldObstacle copy() {
		return new PotentialFieldObstacleOSM(attributes, new LinkedList<>(obstacles));
	}

	@Override
	public void initialize(List<Attributes> attributesList, Topography topography,
			AttributesAgent attributesPedestrian, Random random) {
		// TODO should be used to initialize the Model
	}
}
