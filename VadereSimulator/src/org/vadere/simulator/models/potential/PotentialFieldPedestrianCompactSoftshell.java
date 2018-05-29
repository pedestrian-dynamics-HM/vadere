package org.vadere.simulator.models.potential;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math.util.MathUtils;
import org.vadere.simulator.models.Model;
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
import org.vadere.util.math.MathUtil;

public class PotentialFieldPedestrianCompactSoftshell implements PotentialFieldAgent {

	private AttributesPotentialCompactSoftshell attributes;
	private double intimateWidth;
	private double personalWidth;
	private double height;

	public PotentialFieldPedestrianCompactSoftshell() {}

	@Override
	public void initialize(List<Attributes> attributesList, Topography topography,
	                       AttributesAgent attributesPedestrian, Random random) {
		this.attributes = Model.findAttributes(attributesList, AttributesPotentialCompactSoftshell.class);
		this.intimateWidth = attributes.getPedPotentialIntimateSpaceWidth();
		this.personalWidth = attributes.getPedPotentialPersonalSpaceWidth();
		this.height = attributes.getPedPotentialHeight();
	}

	@Override
	public Collection<Pedestrian> getRelevantAgents(VCircle relevantArea,
			Agent pedestrian, Topography scenario) {
		List<Pedestrian> closePedestrians = scenario.getSpatialMap(Pedestrian.class)
				.getObjects(relevantArea.getCenter(), this.personalWidth + 0.5);
		return closePedestrians;
	}

	@Override
	public double getAgentPotential(VPoint pos, Agent pedestrian,
			Agent otherPedestrian) {

		double radii = pedestrian.getRadius() + otherPedestrian.getRadius();
		double potential = 0;
		double distnaceSq = otherPedestrian.getPosition().distanceSq(pos);
		double maxDistanceSq = (Math.max(personalWidth, intimateWidth)  + radii) * (Math.max(personalWidth, intimateWidth)  + radii);

		if(distnaceSq < maxDistanceSq) {
			double distance = otherPedestrian.getPosition().distance(pos);

			int intPower = this.attributes.getIntimateSpacePower();
			int perPower = this.attributes.getPersonalSpacePower();
			double factor = this.attributes.getIntimateSpaceFactor();

			if (distance < personalWidth + radii) {
				potential += this.height * MathUtil.expAp(4 / (Math.pow(distance / (personalWidth + radii), (2 * perPower)) - 1));
			}
			if (distance < this.intimateWidth + radii) {
				potential += this.height / factor
						* MathUtil.expAp(4 / (Math.pow(distance / (this.intimateWidth + radii), (2 * intPower)) - 1));
			}
			if (distance < radii) {
				potential += 1000 * MathUtil.expAp(1 / (Math.pow(distance / radii, 4) - 1));
			}
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
		throw new UnsupportedOperationException("not jet implemented.");
		/*double epsilon = 0.001;
		double dx = 0;
		double dy = 0;

		VPoint dxPos = pos.add(new VPoint(pos.getX() + MathUtils.EPSILON, pos.getY()));
		VPoint dyPos = pos.add(new VPoint(pos.getX(), pos.getY() + MathUtils.EPSILON));

		double potential = getAgentPotential(pos, pedestrian, otherPedestrians);
		dx = (getAgentPotential(dxPos, pedestrian, otherPedestrians) - potential) / epsilon;
		dy = (getAgentPotential(dyPos, pedestrian, otherPedestrians) - potential) / epsilon;

		return new Vector2D(dx, dy);*/
	}
}
