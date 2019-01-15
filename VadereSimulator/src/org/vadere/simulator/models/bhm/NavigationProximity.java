package org.vadere.simulator.models.bhm;

import org.vadere.state.attributes.models.AttributesBHM;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NavigationProximity implements Navigation {

	private static Logger logger = Logger.getLogger(NavigationProximity.class);

	private final Random random;
	private final AttributesBHM attributesBHM;
	private final PedestrianBHM me;

	public NavigationProximity(PedestrianBHM me, Random random) {
		this.me = me;
		this.random = random;
		this.attributesBHM = me.getAttributesBHM();
	}

	@Override
	public VPoint getNavigationPosition() {

		me.action = 1; // LOGGING

		VPoint result = me.computeTargetStep();

		if (me.evadesTangentially()) {
			Pedestrian collisionPed = me.findCollisionPedestrian(result, false);

			if (collisionPed != null) {

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

		if (me.collidesWithPedestrianOnPath(result) || me.collidesWithObstacle(result) ||
		// make sure that more distance is kept for numerical stability with step or wait heuristic
				(!me.evadesTangentially() &&
						me.collidesWithPedestrian(result, 2 * attributesBHM.getSpaceToKeep()))) {

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
