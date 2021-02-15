package org.vadere.simulator.models.bhm.helpers.navigation;

import org.vadere.simulator.models.bhm.PedestrianBHM;
import org.vadere.simulator.models.bhm.UtilsBHM;
import org.vadere.state.attributes.models.AttributesBHM;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class NavigationFollower implements INavigation {

	private static Logger logger = Logger.getLogger(NavigationFollower.class);

	private Topography topography;
	private AttributesBHM attributesBHM;
	private PedestrianBHM me;
	private NavigationProximity proximityNavigation;

	public void initialize(PedestrianBHM pedestrianBHM, Topography topography, Random random) {
		this.me = pedestrianBHM;
		this.topography = topography;
		this.attributesBHM = me.getAttributesBHM();
		this.proximityNavigation = new NavigationProximity();
		this.proximityNavigation.initialize(me, topography, random);
	}

	@Override
	public VPoint getNavigationPosition() {
		VPoint result;

		Pedestrian futureCollision = me.findCollisionPedestrian(me.computeMovementProjection(), false);

		if (futureCollision != null) {
			result = selectFollowingPosition();

			if (result == null) {

				result = proximityNavigation.getNavigationPosition();

			} else if (me.collidesWithObstacle(result) || me.collidesWithPedestrianOnPath(result)) {

				if (attributesBHM.isFollowerProximityNavigation()) {
					result = proximityNavigation.getNavigationPosition();
				} else {
					result = me.getPosition();
				}
			}
		} else {

			result = proximityNavigation.getNavigationPosition();
		}

		return result;
	}

	private VPoint selectFollowingPosition() {
		VPoint result = null;
		Pedestrian pedestrianToFollow = selectPedestrianToFollow();

		if (pedestrianToFollow != null) {
			VPoint followDirection = pedestrianToFollow.getPosition().subtract(me.getPosition()).norm();
			result = followDirection.scalarMultiply(me.getStepLength()).add(me.getPosition());
		}

		return result;
	}

	private Pedestrian selectPedestrianToFollow() {

		List<Pedestrian> followOptions = new LinkedList<>();

		// select possible pedestrians to follow
		for (Pedestrian other : topography.getElements(Pedestrian.class)) {

			if (other.getId() != me.getId()) {

				if (other.getPosition().distance(me.getPosition()) < attributesBHM.getFollowerDistance()) {

					if (UtilsBHM.angleBetweenTargetDirection(me, other) < attributesBHM.getFollowerAngleMovement()) {

						VPoint directionOther = other.getPosition().subtract(me.getPosition());

						if (UtilsBHM.angle(me.getTargetDirection(), directionOther) < attributesBHM
								.getFollowerAnglePosition()) {
							followOptions.add(other);
						}
					}
				}
			}
		}

		double closestFollowOption = Double.MAX_VALUE;
		Pedestrian pedestrianToFollow = null;

		// select closest pedestrian to follow
		for (Pedestrian other : followOptions) {
			double distanceToOther = other.getPosition().distance(me.getPosition());
			if (distanceToOther < closestFollowOption) {
				pedestrianToFollow = other;
				closestFollowOption = distanceToOther;
			}
		}

		return pedestrianToFollow;
	}

}
