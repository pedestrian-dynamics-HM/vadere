package org.vadere.simulator.models.potential;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesPotentialCompactSoftshell;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

@ModelClass
public class PotentialFieldObstacleCompactSoftshell implements PotentialFieldObstacle {

	private static Logger log = Logger.getLogger(PotentialFieldObstacleCompactSoftshell.class);
	private AttributesPotentialCompactSoftshell attributes;
	private Random random;
	private double width;
	private double height;
	private Collection<Obstacle> obstacles;
	private Domain domain;

	public PotentialFieldObstacleCompactSoftshell() {}

	@Override
	public void initialize(List<Attributes> attributesList, Domain domain,
	                       AttributesAgent attributesPedestrian, Random random) {
		init(Model.findAttributes(attributesList, AttributesPotentialCompactSoftshell.class), domain, random);
	}

	private void init(AttributesPotentialCompactSoftshell attributes, Domain domain, Random random){
		this.attributes = attributes;
		this.width = attributes.getObstPotentialWidth();
		this.height = attributes.getObstPotentialHeight();
		this.random = random;
		this.obstacles = new ArrayList<>(domain.getTopography().getObstacles());
		this.domain = domain;
	}


	@Override
	public double getObstaclePotential(IPoint pos, Agent pedestrian) {

		double potential = 0;
		//for (Obstacle obstacle : obstacles) {

			//double distance = obstacle.getShape().distance(pos);
			double distance = domain.getTopography().distanceToObstacle(pos, pedestrian);

			/*if(distance > 0) {
				log.info("distance: " + distance);
			}*/

			double radius = pedestrian.getRadius();
			double currentPotential = 0;

			if (distance < this.width) {
				currentPotential = this.height * Math.exp(2 / (Math.pow(distance / (this.width), 2) - 1));
			}
			if (distance < radius) {
				currentPotential += 100000 * Math.exp(1 / (Math.pow(distance / radius, 2) - 1));
			}

			if (potential < currentPotential)
				potential = currentPotential;
		//}

		return potential;
	}

	@Override
	public Vector2D getObstaclePotentialGradient(VPoint pos, Agent pedestrian) {
		throw new UnsupportedOperationException("not jet implemented.");
		/*double epsilon = 0.0000001;
		VPoint dxPos = pos.add(new VPoint(pos.getX() + MathUtils.EPSILON, pos.getY()));
		VPoint dyPos = pos.add(new VPoint(pos.getX(), pos.getY() + MathUtils.EPSILON));

		double potential = getObstaclePotential(pos, pedestrian);
		double dx = (getObstaclePotential(dxPos, pedestrian) - potential) / epsilon;
		double dy = (getObstaclePotential(dyPos, pedestrian) - potential) / epsilon;

		return new Vector2D(dx, dy);*/
	}

	@Override
	public PotentialFieldObstacle copy() {
		PotentialFieldObstacleCompactSoftshell potentialFieldObstacle = new PotentialFieldObstacleCompactSoftshell();
		potentialFieldObstacle.init(attributes, domain, random);
		return potentialFieldObstacle;
	}
}
