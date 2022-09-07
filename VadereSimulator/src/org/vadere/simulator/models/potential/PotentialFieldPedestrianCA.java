package org.vadere.simulator.models.potential;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
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

// AgentPotential for the cellular automaton, no repulsion between agents.

@ModelClass
public class PotentialFieldPedestrianCA implements PotentialFieldAgent {

	private final double GRID_BUFFER = 2E-1;
	private final double height_potential = 1000;
	public PotentialFieldPedestrianCA() {}

	@Override
	public void initialize(List<Attributes> attributesList, Domain topography,
	                       AttributesAgent attributesPedestrian, Random random) {

	}

	@Override
	public Collection<Pedestrian> getRelevantAgents(VCircle maxStepCircle,
			Agent pedestrian, Topography scenario) {
		List<Pedestrian> closePedestrians = scenario.getSpatialMap(Pedestrian.class).getObjects(maxStepCircle.getCenter(),
				maxStepCircle.getRadius()*2); // factor 2 to assure that all surrounding relevant pedestrians are found
		return closePedestrians;
	}

	@Override
	public double getAgentPotential(IPoint pos, Agent pedestrian,
	                                Agent otherPedestrian) {

		double radii = pedestrian.getRadius() + otherPedestrian.getRadius(); // 2* r_p (sivers-2016b)
		double potential;

		double distance = otherPedestrian.getPosition().distance(pos); // Euclidean distance d_j(x) between agent j and position x
		if(radii - distance > GRID_BUFFER) {// do not add high potential value for touching agents (BUFFER)
			potential = height_potential;
		}else{
			potential = 0;
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
		throw new UnsupportedOperationException("not yet implemented.");
	}
}
