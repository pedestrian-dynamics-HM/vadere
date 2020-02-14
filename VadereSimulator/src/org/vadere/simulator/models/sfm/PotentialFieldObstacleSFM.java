package org.vadere.simulator.models.sfm;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.simulator.models.potential.solver.gradients.GradientProvider;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesPotentialSFM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Provides gradients for obstacles using the formula of the classical SFM
 * (Helbing 1995).
 * 
 */
@ModelClass
public class PotentialFieldObstacleSFM implements GradientProvider,
		PotentialFieldObstacle {

	private Collection<Obstacle> obstacles;
	private Domain domain;
	private AttributesPotentialSFM attributes;

	public PotentialFieldObstacleSFM() {}

	@Override
	public void initialize(final List<Attributes> attributesList, final Domain domain,
						   final AttributesAgent attributesPedestrian, final Random random) {
		this.obstacles = domain.getTopography().getObstacles();
		this.domain = domain;
		this.attributes = Model.findAttributes(attributesList, AttributesPotentialSFM.class);
	}

	@Override
	public void gradient(double t, int targetID, double[] x, double[] completeGrad) {
		double pot = 0;
		double[] grad = new double[2];

		VPoint closest = new VPoint(0, 0);
		double[] distanceVec = new double[2];
		VPoint position = new VPoint(x[0], x[1]);
		double distance = 0;

		completeGrad[0] = 0;
		completeGrad[1] = 0;

		for (Obstacle obstacle : obstacles) {
			closest = new VPoint(0, 0);

			closest = obstacle.getShape().closestPoint(position);

			distance = position.distance(closest);

			distanceVec[0] = x[0] - closest.x;
			distanceVec[1] = x[1] - closest.y;

			// compute the potential from ped i at x
			pot = attributes.getObstacleBodyPotential()
					* Math.exp(-distance
							/ attributes.getObstacleRepulsionStrength());

			// compute and normalize the gradient length to the potential
			double normDV = Math.sqrt(distanceVec[0] * distanceVec[0]
					+ distanceVec[1] * distanceVec[1]);
			if (normDV > GeometryUtils.DOUBLE_EPS) {
				grad[0] = -distanceVec[0] / normDV * pot;
				grad[1] = -distanceVec[1] / normDV * pot;
			} else {
				grad[0] = 0;
				grad[1] = 0;
			}

			// add to total gradient at x
			completeGrad[0] += grad[0];
			completeGrad[1] += grad[1];
		}
	}

	@Override
	public double getObstaclePotential(IPoint pos, Agent pedestrian) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Vector2D getObstaclePotentialGradient(VPoint pos,
			Agent pedestrian) {

		double[] completeGrad = new double[2];
		double[] x = new double[] {pos.getX(), pos.getY()};
		double t = 0;
		gradient(t, pedestrian.getNextTargetId(), x, completeGrad);

		return new Vector2D(completeGrad[0], completeGrad[1]);
	}

	@Override
	public PotentialFieldObstacleSFM copy() {
		PotentialFieldObstacleSFM potentialFieldObstacle = new PotentialFieldObstacleSFM();
		potentialFieldObstacle.attributes = attributes;
		potentialFieldObstacle.domain = domain;
		potentialFieldObstacle.obstacles = new LinkedList<>(domain.getTopography().getObstacles());
		return potentialFieldObstacle;
	}
}
