package org.vadere.simulator.models.potential;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesPotentialRingExperiment;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;

import java.util.List;
import java.util.Random;

/**
 * A potential field forming a ring.
 * 
 * The ring consists of two circles and is used to simulate the experiment described in jelic-2012
 * and jelic-2012b:
 * ____
 * / __ \
 * / / \ \
 * \ \__/ /
 * \____/
 */

@ModelClass
public class PotentialFieldObstacleRingExperiment implements PotentialFieldObstacle {

	private AttributesPotentialRingExperiment attributes;

	private VPoint center;
	private double radiusInnerCircle;
	private double radiusOuterCircle;

	public PotentialFieldObstacleRingExperiment() {}

	@Override
	public void initialize(List<Attributes> attributesList, Domain topography,
	                       AttributesAgent attributesPedestrian, Random random) {
		init(Model.findAttributes(attributesList, AttributesPotentialRingExperiment.class));
	}

	private void init(final AttributesPotentialRingExperiment attributes) {
		this.attributes = attributes;
		center = attributes.getCenter();

		if (attributes.getRadius1() < attributes.getRadius2()) {
			radiusInnerCircle = attributes.getRadius1();
			radiusOuterCircle = attributes.getRadius2();
		} else {
			radiusInnerCircle = attributes.getRadius2();
			radiusOuterCircle = attributes.getRadius1();
		}
	}

	@Override
	public double getObstaclePotential(IPoint pos, Agent pedestrian) {
		double potential = 0;

		double distanceCenterToPoint = center.distance(pos);

		if (distanceCenterToPoint < radiusInnerCircle || distanceCenterToPoint > radiusOuterCircle) {
			potential = 1000000;
		} else { // Allow only walking on a small track around the desired trajectory.
			double distancePointToTrajectory1 = Math.abs(distanceCenterToPoint - attributes.getPedestrianTrajectory1());
			double distancePointToTrajectory2 = Math.abs(distanceCenterToPoint - attributes.getPedestrianTrajectory2());

			// Pedestrians can walk on left-hand or right-hand side of trajectory.
			double allowedTrajectoryWidth = attributes.getAllowedTrajectoryWidth() / 2;

			if (distancePointToTrajectory1 > allowedTrajectoryWidth
					&& distancePointToTrajectory2 > allowedTrajectoryWidth)
				potential = 100000;
		}

		return potential;
	}

	@Override
	public Vector2D getObstaclePotentialGradient(VPoint pos,
			Agent pedestrian) {
		return new Vector2D(0, 0);
	}

	@Override
	public PotentialFieldObstacle copy() {
		PotentialFieldObstacleRingExperiment potentialFieldObstacleRingExperiment = new PotentialFieldObstacleRingExperiment();
		potentialFieldObstacleRingExperiment.init(attributes);
		return potentialFieldObstacleRingExperiment;
	}
}
