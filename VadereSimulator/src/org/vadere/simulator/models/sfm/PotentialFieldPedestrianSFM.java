package org.vadere.simulator.models.sfm;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesPotentialSFM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.math.MathUtil;

import java.util.Collection;
import java.util.List;
import java.util.Random;

@ModelClass
public class PotentialFieldPedestrianSFM implements PotentialFieldAgent {

	private AttributesPotentialSFM attributes;

	public PotentialFieldPedestrianSFM() {}

	@Override
	public void initialize(List<Attributes> attributesList, Domain topography,
	                       AttributesAgent attributesPedestrian, Random random) {
		this.attributes  = Model.findAttributes(attributesList, AttributesPotentialSFM.class);
	}

	@Override
	public double getAgentPotential(IPoint pos, Agent pedestrian,
	                                Collection<? extends Agent> otherPedestrians) {
		throw new UnsupportedOperationException();
	}

	/**
	 * This is the pedestrian repulsion gradient function for the Gradient
	 * Navigation Model.
	 */
	@Override
	public Vector2D getAgentPotentialGradient(IPoint pos,
			Vector2D velocity, Agent pedestrian,
			Collection<? extends Agent> closePedestrians) {

		double[] completeGrad = new double[2];
		double[] grad = new double[2];
		double[] pedDistance = new double[2];
		double distance;
		double pot;
		double phi;
		double[] x = new double[] {pos.getX(), pos.getY()};
		double[] x2 = new double[2];
		double[] v = new double[] {velocity.x, velocity.y};
		double[] v2 = new double[2];
		double b;
		double stepLength2;
		double dt = 0.2; // constant for step length
		double c = 0.5; // constant for weight when other pedestrian is not in line of sight
		double viewingAngle = Math.PI * 100 / 180;

		for (Agent otherPedestrian : closePedestrians) {
			if (otherPedestrian == pedestrian) {
				continue;
			}

			distance = otherPedestrian.getPosition().distance(pos);
			x2[0] = otherPedestrian.getPosition().x;
			x2[1] = otherPedestrian.getPosition().y;

			pedDistance[0] = x[0] - x2[0];
			pedDistance[1] = x[1] - x2[1];

			v2[0] = otherPedestrian.getVelocity().x;
			v2[1] = otherPedestrian.getVelocity().y;

			// MathUtil.normalize(v2);

			stepLength2 = otherPedestrian.getVelocity().getLength() * dt;

			double distance2 = MathUtil.norm2(new double[] {
					pedDistance[0] - dt * v2[0],
					pedDistance[1] - dt * v2[1]});
			b = 0.5 * Math.sqrt(Math.pow(distance + distance2, 2) - stepLength2
					* stepLength2);

			pot = attributes.getPedestrianBodyPotential()
					* Math.exp(-b
							/ attributes.getPedestrianRecognitionDistance());

			// compute and normalize the gradient length to the
			// potential
			phi = Math.atan2(pedDistance[1], pedDistance[0]);

			grad[0] = -Math.cos(phi) * pot;
			grad[1] = -Math.sin(phi) * pot;

			// line of sight
			double visibility = visibility(c, grad, v, viewingAngle);

			// add to total gradient at x
			completeGrad[0] += grad[0] * visibility;
			completeGrad[1] += grad[1] * visibility;
		}

		return new Vector2D(completeGrad[0], completeGrad[1]);
	}

	private double visibility(double c, double[] fvec, double[] v, double phi) {
		Vector2D e = new Vector2D(v[0], v[1]).normalize(1);
		Vector2D f = new Vector2D(fvec[0], fvec[1]);

		if (e.scalarProduct(f) >= f.getLength() * Math.cos(phi)) {
			return 1;
		} else {
			return c;
		}
	}

	@Override
	public double getAgentPotential(IPoint pos, Agent pedestrian,
			Agent otherPedestrian) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<Agent> getRelevantAgents(VCircle relevantArea,
			Agent center, Topography scenario) {
		List<Agent> closePedestrians = scenario.getSpatialMap(Agent.class)
				.getObjects(relevantArea.getCenter(),
						attributes.getPedestrianRecognitionDistance() + 3);
		// add five meters accounting for the fact that a negative exponential is used, not a
		// function on compact support.
		// => exp(-x) > 0 outside of the "recognition distance" parameter.
		// TODO [priority=medium] [task=bugfix] [Error?] dont call it recognition distance here. call it exp_sigma or sth. else. +3 = magic number?

		return closePedestrians;
	}

	@Override
	public void preLoop(double simTimeInSec) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postLoop(double simTimeInSec) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update(double simTimeInSec) {
		// TODO Auto-generated method stub
		
	}
}
