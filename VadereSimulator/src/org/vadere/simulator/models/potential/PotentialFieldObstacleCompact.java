package org.vadere.simulator.models.potential;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesPotentialCompact;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

@ModelClass
public class PotentialFieldObstacleCompact implements PotentialFieldObstacle {

	private AttributesPotentialCompact attributes;
	private Random random;
	private double width;
	private double height;
	private Collection<Obstacle> obstacles;
	private Domain domain;

	public PotentialFieldObstacleCompact() {

	}

	@Override
	public void initialize(List<Attributes> attributesList, Domain domain,
	                       AttributesAgent attributesPedestrian, Random random) {
		init(Model.findAttributes(attributesList, AttributesPotentialCompact.class), domain, random);
	}

	private void init(final AttributesPotentialCompact attributes, final Domain domain, final Random random) {
		this.attributes = attributes;
		this.domain = domain;
		this.obstacles = new ArrayList<>(domain.getTopography().getObstacles());
		this.random = random;
		this.width = attributes.getObstPotentialWidth() +
				attributes.getObstDistanceDeviation() * (random.nextDouble() * 2 - 1);
		this.height = attributes.getObstPotentialHeight();
	}

	@Override
	public double getObstaclePotential(IPoint pos, Agent pedestrian) {

		double potential = 0;
		//for (Obstacle obstacle : obstacles) {

			//double distance = obstacle.getShape().distance(pos);
			double distance = domain.getTopography().distanceToObstacle(pos);

			if (attributes.isUseHardBodyShell()) {
				distance = distance - pedestrian.getRadius();
			}

			double currentPotential = 0;

			if (distance <= 0) {
				currentPotential = 1000000;
			} else if (distance < this.width) {
				currentPotential = this.height * Math.exp(1 / (Math.pow(distance / this.width, 2) - 1));
			}

			if (potential < currentPotential)
				potential = currentPotential;

		//}

		return potential;
	}

	@Override
	public Vector2D getObstaclePotentialGradient(VPoint pos,
			Agent pedestrian) {

		Vector2D result;

		Obstacle closestObstacle = null;
		double closestDistance = Double.POSITIVE_INFINITY;

		for (Obstacle obstacle : obstacles) {

			double distance = obstacle.getShape().distance(pos);

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

		if (distance >= 0 && distance < this.width) {

			VPoint closestPoint = obs.getShape().closestPoint(pos);
			Vector2D direction = new Vector2D(pos.x - closestPoint.x, pos.y - closestPoint.y);
			direction = direction.normalize(distance);

			double dp = -2 * height * distance * width * width / Math.pow(distance * distance - width * width, 2);
			dp = dp * Math.exp(1 / (distance * distance / (width * width) - 1));

			result = new Vector2D(dp * direction.x, dp * direction.y);
		} else {
			result = new Vector2D(0, 0);
		}

		return result;
	}

	@Override
	public PotentialFieldObstacle copy() {
		PotentialFieldObstacleCompact potentialFieldObstacleCompact = new PotentialFieldObstacleCompact();
		potentialFieldObstacleCompact.init(attributes, domain, random);
		return potentialFieldObstacleCompact;
	}

}
