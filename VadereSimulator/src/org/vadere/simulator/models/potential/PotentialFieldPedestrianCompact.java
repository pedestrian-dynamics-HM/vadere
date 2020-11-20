package org.vadere.simulator.models.potential;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesPotentialCompact;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

// This potential is explained in seitz-2015 (The effect of stepping on pedestrians trajectories)
// p. 596, eq. 1

@ModelClass
public class PotentialFieldPedestrianCompact implements PotentialFieldAgent {

	class DistanceComparator implements Comparator<Agent> {

		private final VPoint position;

		public DistanceComparator(VPoint position) {
			this.position = position;
		}

		@Override
		public int compare(Agent ped1, Agent ped2) {
			double dist1 = this.position.distance(ped1.getPosition());
			double dist2 = this.position.distance(ped2.getPosition());

			if (dist1 < dist2) {
				return -1;
			} else if (dist1 == dist2) {
				return 0;
			} else {
				return 1;
			}
		}
	}

	private AttributesPotentialCompact attributes;
	private AttributesAgent attributesAgent;
	private double width;
	private double height;

	public PotentialFieldPedestrianCompact() {}

	@Override
	public void initialize(List<Attributes> attributesList, Domain domain,
	                       AttributesAgent attributesPedestrian, Random random) {
		this.attributes  = Model.findAttributes(attributesList, AttributesPotentialCompact.class);
		this.attributesAgent = domain.getTopography().getAttributesPedestrian();
		this.width = attributes.getPedPotentialWidth();
		this.height = attributes.getPedPotentialHeight();
	}

	@Override
	public double getMaximalInfluenceRadius() {
		if(attributes.isUseHardBodyShell()) {
			return width + attributesAgent.getRadius();
		}
		else {
			return width;
		}
	}

	@Override
	public Collection<Pedestrian> getRelevantAgents(@NotNull final VCircle stepDisc,
			Agent pedestrian, Topography scenario) {

		// select pedestrians within recognition distance
		return scenario.getSpatialMap(Pedestrian.class)
				.getObjects(stepDisc.getCenter(), stepDisc.getRadius() + this.width + pedestrian.getRadius() + attributes.getVisionFieldRadius());
	}

	@Override
	public double getAgentPotential(IPoint pos, Agent pedestrian,
	                                Agent otherPedestrian) {
		double distance = otherPedestrian.getPosition().distance(pos);


		double potential = 0;

		if (attributes.isUseHardBodyShell()) {
			distance = distance - pedestrian.getRadius() - otherPedestrian.getRadius();
		}

		if (distance < 0) {
			potential = 1000;
		} else if (distance < this.width) {
			potential = this.height * Math.exp(1 / (Math.pow(distance / this.width, 2) - 1));
		}

		return potential;
	}

	@Override
	public double getAgentPotential(IPoint pos, Agent pedestrian,
			Collection<? extends Agent> otherPedestrians) {
		double potential = 0;

		for (Agent neighbor : otherPedestrians) {
			if (neighbor.getId() != pedestrian.getId()) {
				potential += getAgentPotential(pos, pedestrian, neighbor);
			}
		}

		return potential;
	}

	@Override
	public Vector2D getAgentPotentialGradient(IPoint pos,
			Vector2D velocity, Agent pedestrian,
			Collection<? extends Agent> otherPedestrians) {

		Vector2D gradient = new Vector2D(0, 0);

		for (Agent neighbor : otherPedestrians) {
			if (neighbor != pedestrian) {
				gradient = gradient.add(getAgentPotentialGradient(pos,
						pedestrian, neighbor));
			}
		}

		return gradient;
	}

	public Vector2D getAgentPotentialGradient(IPoint pos,
			Agent pedestrian, Agent otherPedestrian) {

		Vector2D result;

		VPoint positionOther = otherPedestrian.getPosition();
		double distance = positionOther.distance(pos);

		if (distance < this.width) {

			Vector2D direction = new Vector2D(pos.getX() - positionOther.x, pos.getY() - positionOther.y);
			direction = direction.normalize(distance);

			double dp = -2 * height * distance * width * width / Math.pow(distance * distance - width * width, 2);
			dp = dp * Math.exp(1 / (distance * distance / (width * width) - 1));

			result = new Vector2D(dp * direction.x, dp * direction.y);
		} else {
			result = new Vector2D(0, 0);
		}
		return result;
	}
}
