package org.vadere.simulator.models.gnm;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesPotentialGNM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.math.MathUtil;

import java.util.Collection;
import java.util.List;
import java.util.Random;

@ModelClass
public class PotentialFieldPedestrianGNM implements PotentialFieldAgent {

	private AttributesPotentialGNM attributes;

	public PotentialFieldPedestrianGNM() {}

	@Override
	public void initialize(List<Attributes> attributesList, Domain topography,
	                       AttributesAgent attributesPedestrian, Random random) {
		this.attributes = Model.findAttributes(attributesList, AttributesPotentialGNM.class);
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
		double visiblePortion;
		double phi;
		double[] x = new double[] {pos.getX(), pos.getY()};
		double[] x2 = new double[2];
		double[] v = new double[] {velocity.x, velocity.y};

		for (Agent otherPedestrian : closePedestrians) {
			if (otherPedestrian == pedestrian) {
				continue;
			}

			distance = otherPedestrian.getPosition().distance(pos);
			x2[0] = otherPedestrian.getPosition().x;
			x2[1] = otherPedestrian.getPosition().y;

			pedDistance[0] = x[0] - x2[0];
			pedDistance[1] = x[1] - x2[1];

			// include h_epsilon to avoid strange behaviour when peds step exactly on top of each
			// other
			if (distance < GeometryUtils.DOUBLE_EPS) {
				pot = 0;
			} else {
				pot = attributes.getPedestrianBodyPotential()
						* MathUtil.cutExp(distance,
								attributes.getPedestrianRecognitionDistance());
			}

			// compute the visible portion of ped i
			visiblePortion = MathUtil.visiblePortion(x, v, x2);

			// compute and normalize the gradient length to the
			// potential
			phi = Math.atan2(pedDistance[1], pedDistance[0]);
			grad[0] = -Math.cos(phi) * pot * visiblePortion;
			grad[1] = -Math.sin(phi) * pot * visiblePortion;

			// add to total gradient at x
			completeGrad[0] += grad[0];
			completeGrad[1] += grad[1];
		}

		return new Vector2D(completeGrad[0], completeGrad[1]);
	}

	@Override
	public double getAgentPotential(IPoint pos, Agent pedestrian,
			Agent otherPedestrian) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<Pedestrian> getRelevantAgents(VCircle relevantArea,
			Agent pedestrian, Topography scenario) {
		List<Pedestrian> closePedestrians = scenario.getSpatialMap(Pedestrian.class)
				.getObjects(relevantArea.getCenter(),
						attributes.getPedestrianRecognitionDistance());

		return closePedestrians;
	}
}
