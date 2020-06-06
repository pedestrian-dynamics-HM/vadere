package org.vadere.simulator.models.potential;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesPotentialOSM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;

import java.util.Collection;
import java.util.List;
import java.util.Random;

@ModelClass
public class PotentialFieldPedestrianOSM implements PotentialFieldAgent {

	private AttributesPotentialOSM attributes;
	private AttributesAgent attributesAgent;

	public PotentialFieldPedestrianOSM() {}

	@Override
	public void initialize(List<Attributes> attributesList, Domain domain,
	                       AttributesAgent attributesPedestrian, Random random) {
		this.attributes = Model.findAttributes(attributesList, AttributesPotentialOSM.class);
		this.attributesAgent = domain.getTopography().getAttributesPedestrian();
	}

	@Override
	public double getMaximalInfluenceRadius() {
		return attributesAgent.getRadius() + attributes.getPedestrianRepulsionWidth();
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
	public double getAgentPotential(IPoint pos, Agent pedestrian,
			Agent otherPedestrian) {
		// Note: Only works for Circle and not for other shapes
		double distance = otherPedestrian.getPosition().distance(pos)
				- pedestrian.getRadius() - otherPedestrian.getRadius();

		double potential = 0;

		if (distance <= 0) {
			potential = attributes.getPedestrianBodyPotential();
		} else if (distance < attributes.getPedestrianRepulsionWidth()) {
			potential = Math.exp(-attributes.getAPedOSM()
					* Math.pow(distance, attributes.getBPedOSM()))
					* attributes.getPedestrianRepulsionStrength();
		}
		return potential;
	}

	@Override
	public Collection<Pedestrian> getRelevantAgents(VCircle relevantArea,
			Agent pedestrian, Topography topography) {
		return topography.getSpatialMap(Pedestrian.class)
				.getObjects(relevantArea.getCenter(),
						attributes.getPedestrianRecognitionDistance());
	}

	@Override
	public Vector2D getAgentPotentialGradient(IPoint pos,
			Vector2D velocity, Agent pedestrian,
			Collection<? extends Agent> otherPedestrians) {

		Vector2D gradient = new Vector2D(0, 0);

		for (Agent neighbor : otherPedestrians) {
			if (neighbor != pedestrian) {
				gradient = gradient.add(getPedestrianPotentialGradient(pos,
						pedestrian, neighbor));
			}
		}

		return gradient;

	}

	public Vector2D getPedestrianPotentialGradient(IPoint pos,
			Agent pedestrian, Agent otherPedestrian) {
		Vector2D result;

		VPoint positionOther = otherPedestrian.getPosition();
		double distance = positionOther.distance(pos) - pedestrian.getRadius()
				- otherPedestrian.getRadius();

		if (distance >= 0
				&& distance < attributes.getPedestrianRepulsionWidth()) {

			Vector2D direction = new Vector2D(pos.getX() - positionOther.x, pos.getY()
					- positionOther.y);
			direction = direction.normalize(distance);

			// part of the gradient that is the same for both vx and vy.
			double vu = -attributes.getAPedOSM()
					* attributes.getBPedOSM()
					* Math.pow(distance, attributes.getBPedOSM() / 2.0 - 1.0)
					* Math.exp(-attributes.getAPedOSM()
							* Math.pow(distance, attributes.getBPedOSM() / 2.0))
					* attributes.getPedestrianRepulsionStrength();

			result = new Vector2D(vu * direction.x, vu * direction.y);
		} else {
			result = new Vector2D(0, 0);
		}

		return result;
	}
}
