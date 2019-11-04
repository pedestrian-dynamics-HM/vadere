package org.vadere.simulator.models.potential;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
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
public class PotentialFieldObstacleCA extends PotentialFieldObstacleCompact implements PotentialFieldObstacle  {

	private Random random;
	private Collection<Obstacle> obstacles;
	private Topography topography;

	public PotentialFieldObstacleCA() {

	}

	@Override
	public void initialize(List<Attributes> attributesList, Topography topography,
						   AttributesAgent attributesPedestrian, Random random) {
		init(topography, random);
	}

	private void init(final Topography topography, final Random random) {
		this.topography = topography;
		this.obstacles = new ArrayList<>(topography.getObstacles());
		this.random = random;
	}

	@Override
	public double getObstaclePotential(IPoint pos, Agent pedestrian) {

		double potential = 0;
		double distance = topography.distanceToObstacle(pos);

		double currentPotential = 0;

		if (distance <= 0) {
			currentPotential = 1000000;
		}

		if (potential < currentPotential)
			potential = currentPotential;

		return potential;
	}


	@Override
	public PotentialFieldObstacle copy() {
		PotentialFieldObstacleCA potentialFieldObstacleCA = new PotentialFieldObstacleCA();
		potentialFieldObstacleCA.init(topography, random);
		return potentialFieldObstacleCA;
	}

}
