package org.vadere.simulator.models.potential;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesPotentialCompactSoftshell;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.Vector2D;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;

public class PotentialFieldPedestrianCompactSoftshell implements PotentialFieldAgent {


	private AttributesPotentialCompactSoftshell attributes;
	private double intimateWidth;
	private double personalWidth;
	private double height;

	public PotentialFieldPedestrianCompactSoftshell(AttributesPotentialCompactSoftshell attributes) {
		this.attributes = attributes;
		this.intimateWidth = attributes.getPedPotentialIntimateSpaceWidth();
		this.personalWidth = attributes.getPedPotentialPersonalSpaceWidth();
		this.height = attributes.getPedPotentialHeight();
	}

	@Override
	public Collection<Pedestrian> getRelevantAgents(VCircle relevantArea,
			Agent pedestrian, Topography scenario) {
		List<Pedestrian> result = new LinkedList<>();

		List<Pedestrian> closePedestrians = scenario.getSpatialMap(Pedestrian.class)
				.getObjects(relevantArea.getCenter(), this.personalWidth + 0.5);

		result = closePedestrians;

		return result;
	}

	@Override
	public double getAgentPotential(VPoint pos, Agent pedestrian,
			Agent otherPedestrian) {
		double distance = otherPedestrian.getPosition().distance(pos);

		int intPower = this.attributes.getIntimateSpacePower();
		int perPower = this.attributes.getPersonalSpacePower();
		double factor = this.attributes.getIntimateSpaceFactor();

		double potential = 0;

		double radii = pedestrian.getRadius() + otherPedestrian.getRadius();

		if (distance < personalWidth + radii) {
			potential += this.height * Math.exp(4 / (Math.pow(distance / (personalWidth + radii), (2 * perPower)) - 1));
		}
		if (distance < this.intimateWidth + radii) {
			potential += this.height / factor
					* Math.exp(4 / (Math.pow(distance / (this.intimateWidth + radii), (2 * intPower)) - 1));
		}
		if (distance < radii) {
			potential += 1000 * Math.exp(1 / (Math.pow(distance / radii, 4) - 1));
		}
		return potential;

	}

	@Override
	public double getAgentPotential(VPoint pos, Agent pedestrian,
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
	public Vector2D getAgentPotentialGradient(VPoint pos,
			Vector2D velocity, Agent pedestrian,
			Collection<? extends Agent> otherPedestrians) {
		throw new UnsupportedOperationException("this method is not jet implemented.");
	}

	@Override
	public void initialize(List<Attributes> attributesList, Topography topography,
			AttributesAgent attributesPedestrian, Random random) {
		// TODO should be used to initialize the Model
	}

}
