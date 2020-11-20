package org.vadere.simulator.models.bhm.helpers.navigation;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.bhm.PedestrianBHM;
import org.vadere.simulator.models.bhm.UtilsBHM;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.utils.topography.TopographyHelper;
import org.vadere.state.attributes.models.AttributesBHM;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class NavigationProximity implements INavigation {

	private static Logger logger = Logger.getLogger(NavigationProximity.class);

	private PedestrianBHM me;
	private AttributesBHM attributesBHM;
	private Topography topography;
	private Random random;

	public void initialize(PedestrianBHM pedestrianBHM, Topography topography, Random random) {
		this.me = pedestrianBHM;
		this.attributesBHM = me.getAttributesBHM();
		this.topography = topography;
		this.random = random;
	}

	private VPoint findSmallerStep(List<Obstacle> collideObstacles, VPoint currentNextPosition) {
		VPoint newNextPosition = currentNextPosition;
		for(Obstacle obstacle : collideObstacles) {
			newNextPosition = findCollisionFreePosition(obstacle, me.getPosition(), newNextPosition);
		}
		return newNextPosition;
	}

	private VPoint findCollisionFreePosition(@NotNull final Obstacle obstacle, VPoint start, VPoint end) {
		VPoint direction = end.subtract(start);
		VShape shape = obstacle.getShape();
		boolean contains = shape.contains(end);
		VPoint closestPoint;

		if(contains) {
			Optional<VPoint> closestIntersectionPoint = shape.getClosestIntersectionPoint(start, end, start);
			// this should never happen!
			if(!closestIntersectionPoint.isPresent()) {
				return end;
			}

			closestPoint = closestIntersectionPoint.get();
		} else {
			closestPoint = shape.closestPoint(end);
		}

		double distance = contains ? -closestPoint.distance(end) : closestPoint.distance(end);
		double diff = me.getRadius() - distance + 0.1;
		assert diff > 0;
		VPoint normal = end.subtract(closestPoint);
		if(contains) {
			normal = normal.scalarMultiply(-1.0);
		}

		VPoint q1 = end.add(normal.setMagnitude(diff));
		VPoint q2 = q1.add(normal.rotate(Math.PI * 0.5));

		VPoint newEnd = GeometryUtils.lineIntersectionPoint(q1, q2, start, end);
		VPoint newDirection = newEnd.subtract(start);

		// the new end generates a shorter step in the same direction?
		if(newDirection.distanceToOrigin() < direction.distanceToOrigin() && direction.subtract(newDirection).distanceToOrigin() < direction.distanceToOrigin()) {
			return newEnd;
		} else {
			return end;
		}
		//return newEnd;
	}

	@Override
	public VPoint getNavigationPosition() {

		me.action = 1; // LOGGING

		VPoint result = me.computeTargetStep();
		boolean targetDirection = true;

		// this is a problem since the ped will never move!
		List<Obstacle> collideObstacles = me.detectObstacleProximity(result, me.getRadius());
		if(attributesBHM.isMakeSmallSteps() && !collideObstacles.isEmpty()) {
			collideObstacles = me.detectObstacleProximity(result, me.getRadius());
			result = findSmallerStep(collideObstacles, result);
		}

		if (me.evadesTangentially()) {
			Pedestrian collisionPed = me.findCollisionPedestrian(result, false);

			if (collisionPed != null && TopographyHelper.otherPedestrianIsCloserToTarget(topography, me, collisionPed)) {
				targetDirection = false;

				// walk away if currently in a collision
				if (me.collidesWithPedestrian(me.getPosition(), attributesBHM.getSpaceToKeep())
						&& attributesBHM.isStepAwayFromCollisions()) {

					logger.warn("Collision with pedestrian " + collisionPed.getId() +
							". Pedestrian " + me.getId() + " stepped away.");

					result = me.stepAwayFromCollision(collisionPed);

					if (!me.collidesWithObstacle(result)) {
						me.action = 12; // LOGGING

						return result;
					}
				} else if (attributesBHM.isOnlyEvadeContraFlow()) {

					double angleBetween = UtilsBHM.angleBetweenMovingDirection(me, collisionPed);

					if (angleBetween > attributesBHM.getOnlyEvadeContraFlowAngle()) {
						result = evadeCollision(collisionPed);

					}
				} else {
					result = evadeCollision(collisionPed);
				}
			}
		}


		/*
		 * Make no step if:
		 * 1) there would be a collision with another pedestrian
		 * 2) there would be a collision with an obstacle and me does not walk in the origin target direction.
		 *      Remark: if we would not allow collisions me would never move again
		 * 3) me does not evade tangentially and there is a collision with another pedestrian in a larger area.
		 *      Remark: this is for numerical stability of the step or wait heuristic
		 */
		if (me.collidesWithPedestrianOnPath(result) ||
				(me.collidesWithObstacle(result) && !targetDirection) ||
				(!me.evadesTangentially() &&  me.collidesWithPedestrian(result, 2 * attributesBHM.getSpaceToKeep()))) {

			/*if( me.collidesWithObstacle(result) ) {
				System.out.println("obs collision " + me.getId());
			}*/

			result = me.getPosition();
			me.action = 0; // LOGGING
		}

		return result;
	}

	private VPoint evadeCollision(Pedestrian collisionPed) {

		VPoint result;

		VPoint relativeThisPosition = me.getPosition().subtract(collisionPed.getPosition());
		double radius = me.getRadius() + collisionPed.getRadius() +
				attributesBHM.getSpaceToKeep() + UtilsBHM.DOUBLE_EPSILON;

		List<VPoint> relativeEvasionPoints;

		if (me.getPosition().distance(collisionPed.getPosition()) < radius) {
			// evade to the sides if currently in a collision to avoid errors when computing the
			// tangential points
			relativeEvasionPoints = new ArrayList<VPoint>(2);
			relativeEvasionPoints.add(0, relativeThisPosition.rotate(Math.PI / 2));
			relativeEvasionPoints.add(1, relativeThisPosition.rotate(-Math.PI / 2));

		} else {
			List<VPoint> tangentialPoints = UtilsBHM.getTangentialPoints(relativeThisPosition, radius);
			relativeEvasionPoints = UtilsBHM.getRelativeEvasionPointFromTangential(
					relativeThisPosition, tangentialPoints);
		}

		List<VPoint> evasionPoints = new ArrayList<VPoint>(2);

		evasionPoints.add(relativeEvasionPoints.get(0).norm().scalarMultiply(me.getStepLength()).add(me.getPosition()));
		evasionPoints.add(relativeEvasionPoints.get(1).norm().scalarMultiply(me.getStepLength()).add(me.getPosition()));

		// DEBUG
		if (Double.isNaN(evasionPoints.get(0).x) || Double.isNaN(evasionPoints.get(0).y) ||
				Double.isNaN(evasionPoints.get(1).x) || Double.isNaN(evasionPoints.get(1).y)) {

			logger.error("Tangential point NaN for pedestrian " + collisionPed.getId() + ".");

			// should not happen
			result = me.getPosition();

		} else {
			result = selectDetour(evasionPoints);

			me.action = 2; // LOGGING

			// evade to the side if the positions are already taken
			if (me.evadesSideways() && me.collidesWithPedestrianOnPath(result)) {

				me.action = 3; // LOGGING

				VPoint targetDirection = me.getTargetDirection();

				evasionPoints.set(0, me.getPosition().add(
						targetDirection.rotate(Math.PI / 2).norm().scalarMultiply(me.getStepLength())));
				evasionPoints.set(1, me.getPosition().add(
						targetDirection.rotate(-Math.PI / 2).norm().scalarMultiply(me.getStepLength())));

				result = selectDetour(evasionPoints);
			}
		}

		return result;
	}

	private VPoint selectDetour(List<VPoint> evasionPoints) {

		VPoint result;

		VPoint targetStep = me.computeTargetStep();
		double detour1 = targetStep.distance(evasionPoints.get(0));
		double detour2 = targetStep.distance(evasionPoints.get(1));

		// randomly select left or right detour if difference is below threshold
		if (detour1 < detour2 + attributesBHM.getEvasionDetourThreshold() &&
				detour2 < detour1 + attributesBHM.getEvasionDetourThreshold()) {

			boolean leftRightSwitch = random.nextBoolean();

			if (leftRightSwitch) {
				result = detourCollisionSwitch(evasionPoints.get(0), evasionPoints.get(1));
			} else {
				result = detourCollisionSwitch(evasionPoints.get(1), evasionPoints.get(0));
			}
		} else if (detour1 < detour2) {
			result = detourCollisionSwitch(evasionPoints.get(0), evasionPoints.get(1));
		} else {
			result = detourCollisionSwitch(evasionPoints.get(1), evasionPoints.get(0));
		}

		return result;
	}

	private VPoint detourCollisionSwitch(VPoint evasionPoint1, VPoint evasionPoint2) {
		VPoint result;

		if (me.collidesWithPedestrianOnPath(evasionPoint1) || me.collidesWithObstacle(evasionPoint1)) {
			result = evasionPoint2;
		} else {
			result = evasionPoint1;
		}

		return result;
	}
}
