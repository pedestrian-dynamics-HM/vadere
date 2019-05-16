package org.vadere.simulator.models.bhm;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.attributes.models.AttributesBHM;
import org.vadere.state.scenario.Obstacle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.util.List;

public class DirectionAddendObstacle implements DirectionAddend {

	private static Logger logger = Logger.getLogger(DirectionAddendObstacle.class);

	private final AttributesBHM attributesBHM;
	private final PedestrianBHM me;

	public DirectionAddendObstacle(PedestrianBHM me) {
		this.me = me;
		this.attributesBHM = me.getAttributesBHM();
	}

	@Override
	public VPoint getDirectionAddend(@NotNull final VPoint targetDirection) {
		return getTargetObstacleDirection();
	}

	public VPoint getTargetObstacleDirection() {

		VPoint result = VPoint.ZERO;

		List<Obstacle> closeObstacles = me.detectObstacleProximity(me.getPosition(),
				me.getRadius() + attributesBHM.getObstacleRepulsionReach());

		double normFactor = attributesBHM.getObstacleRepulsionMaxWeight() /
				sumOfObstacleWeights(closeObstacles);

		for (Obstacle obstacle : closeObstacles) {
			double weight = weightObstacleDistance(obstacle.getShape().distance(me.getPosition()) + me.getRadius());

			if (weight > 0.001 && normFactor > 0.001) {

				weight = weight * weight * normFactor;

				VPoint direction = me.getPosition().subtract(
						obstacle.getShape().closestPoint(me.getPosition())).norm();

				result = result.add(direction.scalarMultiply(weight));
			}
		}

		return result;
	}

	private double sumOfObstacleWeights(List<Obstacle> closeObstacles) {
		double result = 0;

		for (Obstacle obstacle : closeObstacles) {
			result = result + weightObstacleDistance(obstacle.getShape().distance(me.getPosition()) + me.getRadius());
		}

		return result;
	}

	private double weightObstacleDistance(double distance) {

		double result = 0;

		if (distance < attributesBHM.getObstacleRepulsionReach()) {
			result = attributesBHM.getObstacleRepulsionReach() - distance;
			result = result / attributesBHM.getObstacleRepulsionReach();
		}

		return result;
	}
}
