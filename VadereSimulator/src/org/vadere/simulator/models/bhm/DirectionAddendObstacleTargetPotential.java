package org.vadere.simulator.models.bhm;

import org.jetbrains.annotations.NotNull;
import org.vadere.state.attributes.models.AttributesBHM;
import org.vadere.state.scenario.Obstacle;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.List;
import java.util.Optional;

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

		Optional<Obstacle> closeObstacles = me.detectClosestObstacleProximity(me.getPosition(), me.getRadius());

		if(closeObstacles.isPresent()) {
			Obstacle obstacle = closeObstacles.get();
			VPoint closestPoint = obstacle.getShape().closestPoint(me.getPosition());
			
		}


		return addend;
	}
}
