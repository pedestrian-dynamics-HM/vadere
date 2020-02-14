package org.vadere.simulator.models.potential;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.IPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

@ModelClass
public class PotentialFieldObstacleCA extends PotentialFieldObstacleCompact implements PotentialFieldObstacle  {

	private Random random;
	private Collection<Obstacle> obstacles;
	private Domain domain;

	public PotentialFieldObstacleCA() {

	}

	@Override
	public void initialize(List<Attributes> attributesList, Domain domain,
	                       AttributesAgent attributesPedestrian, Random random) {
		init(domain, random);
	}

	private void init(final Domain domain, final Random random) {
		this.domain = domain;
		this.obstacles = new ArrayList<>(domain.getTopography().getObstacles());
		this.random = random;
	}

	@Override
	public double getObstaclePotential(IPoint pos, Agent pedestrian) {

		double potential = 0;
		double distance = domain.getTopography().distanceToObstacle(pos);

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
		potentialFieldObstacleCA.init(domain, random);
		return potentialFieldObstacleCA;
	}

}
