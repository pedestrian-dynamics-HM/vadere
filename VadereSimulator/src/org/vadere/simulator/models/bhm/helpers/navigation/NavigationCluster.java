package org.vadere.simulator.models.bhm.helpers.navigation;

import org.vadere.simulator.models.bhm.PedestrianBHM;
import org.vadere.simulator.models.bhm.UtilsBHM;
import org.vadere.state.attributes.models.AttributesBHM;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class NavigationCluster implements INavigation {

	private static Logger logger = Logger.getLogger(NavigationCluster.class);

	private Topography topography;
	private AttributesBHM attributesBHM;
	private PedestrianBHM me;
	private NavigationProximity proximityNavigation;

	@Override
	public void initialize(PedestrianBHM pedestrianBHM, Topography topography, Random random) {
		this.me = pedestrianBHM;
		this.topography = topography;
		this.attributesBHM = me.getAttributesBHM();
		this.proximityNavigation = new NavigationProximity();
		proximityNavigation.initialize(me, topography, random);
	}

	@Override
	public VPoint getNavigationPosition() {

		VPoint result;

		Pedestrian futureCollision = me.findCollisionPedestrian(me.computeMovementProjection(), false);

		if (futureCollision != null) {
			result = evadeCollision(futureCollision);
		} else {
			result = me.computeTargetStep();
		}

		// no good, stay where you are
		if (me.collidesWithPedestrian(result, 0) || me.collidesWithObstacle(result)) {
			// result = me.getPosition();
			result = proximityNavigation.getNavigationPosition();
		}

		return result;
	}

	private VPoint evadeCollision(final Pedestrian collisionPed) {

		VPoint result = null;

		List<Pedestrian> cluster = determineCluster(collisionPed);
		cluster.remove(collisionPed);

		Pedestrian avoidPedestrianMin = collisionPed;
		Pedestrian avoidPedestrianMax = collisionPed;

		List<VPoint> evasionPoints = getRelativeEvasionPoints(collisionPed);

		// pedestrians are in collision state
		if (evasionPoints == null) {
			logger.warn("Collision with pedestrian " + collisionPed.getId() +
					". Pedestrian " + me.getId() + " stepped away.");

			return me.stepAwayFromCollision(collisionPed);
		}

		double angleToTarget1 = UtilsBHM.angleTo(me.getTargetDirection(), evasionPoints.get(0).norm());
		double angleToTarget2 = UtilsBHM.angleTo(me.getTargetDirection(), evasionPoints.get(1).norm());

		double extremeMinAngle, extremeMaxAngle;
		VPoint extremeMinEvasionPoint, extremeMaxEvasionPoint;

		if (angleToTarget1 < angleToTarget2) {
			extremeMinAngle = angleToTarget1;
			extremeMaxAngle = angleToTarget2;
			extremeMinEvasionPoint = evasionPoints.get(0);
			extremeMaxEvasionPoint = evasionPoints.get(1);
		} else {
			extremeMinAngle = angleToTarget2;
			extremeMaxAngle = angleToTarget1;
			extremeMinEvasionPoint = evasionPoints.get(1);
			extremeMaxEvasionPoint = evasionPoints.get(0);
		}

		for (Pedestrian ped : cluster) {
			evasionPoints = getRelativeEvasionPoints(ped);

			// pedestrians are in collision state
			if (evasionPoints == null) {
				logger.warn("Collision with pedestrian " + ped.getId() +
						". Pedestrian " + me.getId() + " stepped away.");

				return me.stepAwayFromCollision(ped);
			}

			angleToTarget1 = UtilsBHM.angleTo(me.getTargetDirection(), evasionPoints.get(0).norm());
			angleToTarget2 = UtilsBHM.angleTo(me.getTargetDirection(), evasionPoints.get(1).norm());

			double angleToTargetThisMin, angleToTargetThisMax;
			VPoint evasionPointMin, evasionPointMax;

			// only the greater angle3D is a candidate for a new extreme value
			if (angleToTarget1 < angleToTarget2) {
				angleToTargetThisMin = angleToTarget1;
				angleToTargetThisMax = angleToTarget2;
				evasionPointMin = evasionPoints.get(0);
				evasionPointMax = evasionPoints.get(1);
			} else {
				angleToTargetThisMin = angleToTarget2;
				angleToTargetThisMax = angleToTarget1;
				evasionPointMin = evasionPoints.get(1);
				evasionPointMax = evasionPoints.get(0);
			}

			if (angleToTargetThisMin < extremeMinAngle) {
				extremeMinAngle = angleToTargetThisMin;
				extremeMinEvasionPoint = evasionPointMin;
				avoidPedestrianMin = ped;
			}
			if (angleToTargetThisMax > extremeMaxAngle) {
				extremeMaxAngle = angleToTargetThisMax;
				extremeMaxEvasionPoint = evasionPointMax;
				avoidPedestrianMax = ped;
			}
		}

		result = selectClusterDetourStep(extremeMinEvasionPoint, extremeMaxEvasionPoint);

		// DEBUG
		// logger.info("Pedestrians from cluster to avoid are " + avoidPedestrianMin.getId() + " and
		// " + avoidPedestrianMax.getId());

		return result;
	}

	private VPoint selectClusterDetourStep(VPoint evasionPoint1, VPoint evasionPoint2) {

		VPoint result;

		VPoint evasionStep1 = evasionPoint1.norm().scalarMultiply(me.getStepLength()).add(me.getPosition());
		VPoint evasionStep2 = evasionPoint2.norm().scalarMultiply(me.getStepLength()).add(me.getPosition());


		// in case of collision return other
		if (me.collidesWithObstacle(evasionStep1)) {

			if (me.collidesWithObstacle(evasionStep2)) {
				// both positions are colliding, fall back to current position
				return me.getPosition();
			} else {
				return evasionStep2;
			}
		} else if (me.collidesWithObstacle(evasionStep2)) {
			return evasionStep1;
		}


		VShape target = topography.getTarget(me.getNextTargetId()).getShape();

		double lastDirectionAngle1 = UtilsBHM.angle(me.getTargetDirection(), evasionPoint1);
		double lastDirectionAngle2 = UtilsBHM.angle(me.getTargetDirection(), evasionPoint2);

		// compute walking distance to target through evasion points
		double detour1 = target.distance(me.getPosition().add(evasionPoint1))
				+ evasionPoint1.distanceToOrigin();
		double detour2 = target.distance(me.getPosition().add(evasionPoint2))
				+ evasionPoint2.distanceToOrigin();


		// avoid walking backwards
		if (lastDirectionAngle1 > attributesBHM.getBackwardsAngle()) {

			if (lastDirectionAngle2 > attributesBHM.getBackwardsAngle())
				if (detour1 < detour2) {
					result = evasionStep1;
				} else {
					result = evasionStep2;
				}
			else {
				result = evasionStep2;
			}
		} else if (lastDirectionAngle2 > attributesBHM.getBackwardsAngle()) {
			result = evasionStep1;
		} else {
			// choose shorter detour to target
			if (detour1 < detour2) {
				result = evasionStep1;
			} else {
				result = evasionStep2;
			}
		}

		return result;
	}

	private List<VPoint> getRelativeEvasionPoints(final Pedestrian collisionPed) {

		List<VPoint> result;

		VPoint relativeThisPosition = me.getPosition().subtract(collisionPed.getPosition());
		double radius = me.getRadius() + collisionPed.getRadius() + UtilsBHM.DOUBLE_EPSILON;

		// pedestrians are in collision state
		if (relativeThisPosition.distanceToOrigin() < radius) {
			result = null;

		} else {
			List<VPoint> tangentialPoints = UtilsBHM.getTangentialPoints(relativeThisPosition, radius);

			// DEBUG
			if (Double.isNaN(tangentialPoints.get(0).x) || Double.isNaN(tangentialPoints.get(0).y) ||
					Double.isNaN(tangentialPoints.get(1).x) || Double.isNaN(tangentialPoints.get(1).y)) {

				logger.error("Tangential point NaN for pedestrian " + collisionPed.getId() + ".");
			}

			result = UtilsBHM.getRelativeEvasionPointFromTangential(
					relativeThisPosition, tangentialPoints);
		}

		return result;
	}

	private List<Pedestrian> determineCluster(Pedestrian collisionPed) {

		List<Pedestrian> result = new LinkedList<>();

		LinkedList<Pedestrian> contained = new LinkedList<>();
		List<Pedestrian> notContained;
		List<Pedestrian> notContainedNext = new LinkedList<>();


		for (Pedestrian other : topography.getElements(Pedestrian.class)) {

			// select pedestrians ahead
			if (UtilsBHM.angleBetweenTarget(me, other) < attributesBHM.getBackwardsAngle()) {

				// skip this and collision pedestrian (the latter is first to be investigated)
				if (other.getId() != me.getId() && other.getId() != collisionPed.getId()) {
					notContainedNext.add(other);
				}
			}
		}

		Pedestrian containedNext = collisionPed;

		while (containedNext != null) {

			notContained = notContainedNext;
			notContainedNext = new LinkedList<>();
			result.add(containedNext);

			// iterate through all pedestrians outside of the cluster
			for (Pedestrian pedNotContained : notContained) {

				// add pedestrian to cluster, who is closer to the cluster than this pedestrian's
				// diameter
				if (containedNext.getPosition().distance(pedNotContained.getPosition()) < containedNext.getRadius()
						+ pedNotContained.getRadius() +
						me.getRadius() * 2 + attributesBHM.getDistanceToKeep()) {

					if (attributesBHM.isOnlyEvadeContraFlow()) {

						double angleBetween = UtilsBHM.angleBetweenTargetDirection(me, pedNotContained);

						if (angleBetween > attributesBHM.getOnlyEvadeContraFlowAngle()) {
							contained.add(pedNotContained);
						}
					} else {
						contained.add(pedNotContained);
					}
				} else {
					notContainedNext.add(pedNotContained);
				}
			}

			if (contained.isEmpty()) {
				containedNext = null;
			} else {
				containedNext = contained.pop();
			}
		}

		return result;
	}
}
