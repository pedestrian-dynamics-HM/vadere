package org.vadere.simulator.models.bhm;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.attributes.models.AttributesBHM;
import org.vadere.state.scenario.Obstacle;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import java.util.List;
import java.util.Optional;

/**
 * Computes a vector to add to the target direction such that collisions with obstacles
 * will be avoided.
 *
 * @author Benedikt Zoennchen
 */
public class DirectionAddendObstacleTargetPotential implements DirectionAddend {

	private final AttributesBHM attributesBHM;
	private final PedestrianBHM me;

	public DirectionAddendObstacleTargetPotential(PedestrianBHM me) {
		this.me = me;
		this.attributesBHM = me.getAttributesBHM();
	}

	@Override
	public VPoint getDirectionAddend(@NotNull final VPoint targetDirection) {
		VPoint addend = VPoint.ZERO;

		VPoint footStep = targetDirection.scalarMultiply(me.getStepLength());


		// compute the next position without changing the target direction.
		VPoint nextPosition = (me.getPosition().add(footStep));


		// get the obstacle closest to the nextPosition causing a collision
		Optional<Obstacle> closeObstacles = me.detectClosestObstacleProximity(nextPosition, me.getRadius() + GeometryUtils.DOUBLE_EPS);


		// if there is none, there is no need to change the target direction
		if(closeObstacles.isPresent()) {
			closeObstacles = me.detectClosestObstacleProximity(me.getPosition(), me.getRadius() + footStep.distanceToOrigin() + GeometryUtils.DOUBLE_EPS);
			Obstacle obstacle = closeObstacles.get();

			// compute the point of the obstacle shape closest to the pedestrian position
			VPoint closestPoint = obstacle.getShape().closestPoint(me.getPosition());

			// compute the normal of the closest line (here we assume the obstacle is in fact a polygon!)
			VPoint normal = closestPoint.subtract(me.getPosition());

			// project the target direction onto the normal
			IPoint p = GeometryUtils.projectOnto(targetDirection.getX(), targetDirection.getY(), normal.x, normal.y);

			// if the target direction points away from the obstacle don't adjust it
			if(!p.equals(VPoint.ZERO)/* && p.norm().distance(normal.norm()) < GeometryUtils.DOUBLE_EPS*/) {

				// if the target direction points in the opposite direction
				if(targetDirection.subtract(p).distanceToOrigin() < GeometryUtils.DOUBLE_EPS) {
					VPoint lastFootStep = me.getPosition().subtract(me.getLastPosition());
					addend = lastFootStep.norm();
				}
				else {
					addend = new VPoint(p.scalarMultiply(-1.0));
				}
			}
		}

		VPoint newTargetDirection = targetDirection.add(addend).norm();
		VPoint newFootStep = newTargetDirection.scalarMultiply(me.getStepLength());
		closeObstacles = me.detectClosestObstacleProximity(me.getPosition().add(newFootStep), me.getRadius());

		if(closeObstacles.isPresent()) {
			VPoint lastFootStep = me.getPosition().subtract(me.getLastPosition());
			addend = targetDirection.scalarMultiply(-1.0).add(lastFootStep.norm());
		}



		return addend;
	}

	private VPoint getClosestPoint(VShape shape, VPoint start, VPoint end) {
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
		return closestPoint;
	}
}
