package org.vadere.simulator.models.potential;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesPotentialCompactSoftshell;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.Vector2D;

import java.util.Collection;
import java.util.List;
import java.util.Random;

// Implementation of the soft shell repulsive potential of pedestrians according to sivers-2016b.
// page 46, eq. 4.1

@ModelClass
public class PotentialFieldPedestrianCompactSoftshell implements PotentialFieldAgent {

	public AttributesPotentialCompactSoftshell getAttributes() {
		return attributes;
	}

	private AttributesPotentialCompactSoftshell attributes;
	private AttributesAgent attributesAgent;
	private double intimateWidth; // radius of intimate zone \delta_{int}
	private double personalWidth; // radius of personal width \delta_{per}



	private double height; // intensity of repulsion \mu_p

	public PotentialFieldPedestrianCompactSoftshell() {}

	@Override
	public void initialize(List<Attributes> attributesList, Domain domain,
	                       AttributesAgent attributesPedestrian, Random random) {
		this.attributes = Model.findAttributes(attributesList, AttributesPotentialCompactSoftshell.class);
		this.attributesAgent = domain.getTopography().getAttributesPedestrian();
		this.intimateWidth = attributes.getPedPotentialIntimateSpaceWidth();
		this.personalWidth = attributes.getPedPotentialPersonalSpaceWidth();
		this.height = attributes.getPedPotentialHeight();
	}

	@Override
	public double getMaximalInfluenceRadius() {
		return Math.max(personalWidth, intimateWidth) + attributesAgent.getRadius();
	}

	@Override
	public Collection<Pedestrian> getRelevantAgents(VCircle maxStepCircle,
			Agent pedestrian, Topography scenario) {
		List<Pedestrian> closePedestrians = scenario.getSpatialMap(Pedestrian.class).getObjects(maxStepCircle.getCenter(),
				this.personalWidth + maxStepCircle.getRadius() + pedestrian.getRadius());
		return closePedestrians;
	}


	public double getPersonalWidth() { return personalWidth; }
	public void setPersonalWidth(final double personalWidth) { this.personalWidth = personalWidth; }
	public double getHeight() { return height; }
	public void setHeight(final double height) { this.height = height; }

	@Override
	public double getAgentPotential(IPoint pos, Agent pedestrian,
									Agent otherPedestrian) {

		double width = getPersonalWidth();
		double height = getHeight();
		return getAgentPotential(pos, pedestrian, otherPedestrian, height, width);

	}


	public double getAgentPotential(IPoint pos, Agent pedestrian,
	                                Agent otherPedestrian, double height, double width) {

		double radii = pedestrian.getRadius() + otherPedestrian.getRadius(); // 2* r_p (sivers-2016b)
		double potential = 0;
		double distanceSq = otherPedestrian.getPosition().distanceSq(pos);
		double maxDistanceSq = (Math.max(width, intimateWidth)  + radii) * (Math.max(width, intimateWidth)  + radii);

		if (distanceSq < maxDistanceSq) {
			double distance = otherPedestrian.getPosition().distance(pos); // Euclidean distance d_j(x) between agent j and position x

			int intPower = this.attributes.getIntimateSpacePower(); // b_p
			int perPower = this.attributes.getPersonalSpacePower(); // not defined in sivers-2016b (perPower = 1)
			double factor = this.attributes.getIntimateSpaceFactor(); // a_p

			if (distance < width + radii) {
				// implementation differs from sivers-2016b here:  \delta_{per} + r_p  (note: radii = 2*r_p)
				potential += height * Math.exp(4 / (Math.pow(distance / (width + radii), (2 * perPower)) - 1));
			}
			if (distance < this.intimateWidth + radii) {
				// implementation differs from sivers-2016b here:  \delta_{int} + r_p  (note: radii = 2*r_p)
				potential += height / factor
						* Math.exp(4 / (Math.pow(distance / (this.intimateWidth + radii), (2 * intPower)) - 1));
			}
			if (distance < radii) {
				// implementations differs from sivers-2016b here : Math.power(distance / (radii),2)
				potential += 1000 * Math.exp(1 / (Math.pow(distance / radii, 4) - 1));
			}
		}
		return potential;

	}


	/*@Override
	public double getAgentPotential(IPoint pos, Agent pedestrian,
			Agent otherPedestrian) {
		double radii = pedestrian.getRadius() + otherPedestrian.getRadius(); // 2* r_p (sivers-2016b)
		double potential = 0;
		double distanceSq = otherPedestrian.getPosition().distanceSq(pos);
		double maxDistanceSq = (Math.max(personalWidth, intimateWidth)  + pedestrian.getRadius()) * (Math.max(personalWidth, intimateWidth)  + pedestrian.getRadius());

		if (distanceSq < maxDistanceSq) {
			double distance = otherPedestrian.getPosition().distance(pos); // Euclidean distance d_j(x) between agent j and position x

			int intPower = this.attributes.getIntimateSpacePower(); // b_p
			int perPower = this.attributes.getPersonalSpacePower(); // not defined in sivers-2016b (perPower = 1)
			double factor = this.attributes.getIntimateSpaceFactor(); // a_p

			if (distance < personalWidth + pedestrian.getRadius()) {
				// implementation differs from sivers-2016b here:  \delta_{per} + r_p  (note: radii = 2*r_p)
				potential += this.height * Math.exp(4 / (Math.pow(distance / (personalWidth + pedestrian.getRadius()), (2 * perPower)) - 1));
			}
			if (distance < this.intimateWidth + pedestrian.getRadius()) {
				// implementation differs from sivers-2016b here:  \delta_{int} + r_p  (note: radii = 2*r_p)
				potential += this.height / factor
						* Math.exp(4 / (Math.pow(distance / (this.intimateWidth + pedestrian.getRadius()), (2 * intPower)) - 1));
			}
			if (distance < radii) {
				// implementations differs from sivers-2016b here : Math.power(distance / (radii),2)
				potential += 1000 * Math.exp(1 / (Math.pow(distance / radii, 2) - 1));
			}
		}
		return potential;
	}*/

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
		throw new UnsupportedOperationException("not yet implemented.");
		/*double epsilon = 0.001;
		double dx = 0;
		double dy = 0;

		double potential = getAgentPotential(pos, pedestrian, otherPedestrians);
		dx = (getAgentPotential(dxPos, pedestrian, otherPedestrians) - potential) / epsilon;
		dy = (getAgentPotential(dyPos, pedestrian, otherPedestrians) - potential) / epsilon;

		return new Vector2D(dx, dy);*/
	}
}
