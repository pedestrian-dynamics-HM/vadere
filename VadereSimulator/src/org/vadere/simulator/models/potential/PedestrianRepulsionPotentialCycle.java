package org.vadere.simulator.models.potential;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Teleporter;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;

import java.util.Collection;
import java.util.List;
import java.util.Random;

@ModelClass
public class PedestrianRepulsionPotentialCycle implements
		PotentialFieldAgent {

	private final Topography scenario;
	private PotentialFieldAgent potentialFieldPedestrian;

	public PedestrianRepulsionPotentialCycle(
			PotentialFieldAgent potentialFieldPedestrian,
			Topography scenario) {
		this.potentialFieldPedestrian = potentialFieldPedestrian;
		this.scenario = scenario;
	}

	@Override
	public double getAgentPotential(IPoint pos, Agent pedestrian,
	                                Collection<? extends Agent> closePedestrians) {

		double result = potentialFieldPedestrian.getAgentPotential(pos,
				pedestrian, closePedestrians);

		if (this.scenario.hasTeleporter()) {

			Teleporter teleporter = scenario.getTeleporter();
			// shift forwards
			VPoint shiftPos = new VPoint(pos.getX()
					+ teleporter.getTeleporterShift().x, pos.getY()); // TODO [priority=medium] [task=feature] the y coordinate of the teleporter is not used yet

			// TODO [priority=low] [task=refactoring] find a better way to get the close pedestrians in this case
			closePedestrians = potentialFieldPedestrian.getRelevantAgents(
					new VCircle(shiftPos, 0.1), pedestrian, scenario);

			result += potentialFieldPedestrian.getAgentPotential(shiftPos,
					pedestrian, closePedestrians);

			// shift backwards
			shiftPos = new VPoint(pos.getX() - teleporter.getTeleporterShift().x,
					pos.getY()); // TODO [priority=low] [task=refactoring] the y coordinate of the teleporter is not used yet

			// TODO [task=refactoring] [priority=low] find a better way to get the close pedestrians in this case
			closePedestrians = potentialFieldPedestrian.getRelevantAgents(
					new VCircle(shiftPos, 0.1), pedestrian, scenario);

			result += potentialFieldPedestrian.getAgentPotential(shiftPos,
					pedestrian, closePedestrians);
		}

		return result;
	}

	@Override
	public Vector2D getAgentPotentialGradient(IPoint pos,
			Vector2D velocity, Agent pedestrian,
			Collection<? extends Agent> closePedestrians) {
		Vector2D result = potentialFieldPedestrian
				.getAgentPotentialGradient(pos, velocity, pedestrian,
						closePedestrians);

		if (this.scenario.hasTeleporter()) {

			Teleporter teleporter = scenario.getTeleporter();
			// shift forwards
			VPoint shiftPos = new VPoint(pos.getX()
					+ teleporter.getTeleporterShift().x, pos.getY()
							+ teleporter.getTeleporterShift().y);

			// TODO [priority=low] [task=refactoring] find a better way to get the close pedestrians in this case
			closePedestrians = potentialFieldPedestrian.getRelevantAgents(
					new VCircle(shiftPos, 0.1), pedestrian, scenario);

			result = result.add(potentialFieldPedestrian
					.getAgentPotentialGradient(shiftPos, velocity,
							pedestrian, closePedestrians));

			// shift backwards
			shiftPos = new VPoint(pos.getX() - teleporter.getTeleporterShift().x,
					pos.getY() - teleporter.getTeleporterShift().y);

			// TODO [priority=low] [task=refactoring] find a better way to get the close pedestrians in this case
			closePedestrians = potentialFieldPedestrian.getRelevantAgents(
					new VCircle(shiftPos, 0.1), pedestrian, scenario);

			result = result.add(potentialFieldPedestrian
					.getAgentPotentialGradient(shiftPos, velocity,
							pedestrian, closePedestrians));
		}

		return result;
	}

	@Override
	public double getAgentPotential(IPoint pos, Agent pedestrian,
			Agent otherPedestrian) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getMaximalInfluenceRadius() {
		return potentialFieldPedestrian.getMaximalInfluenceRadius();
	}

	@Override
	public Collection<? extends Agent> getRelevantAgents(VCircle relevantArea,
			Agent pedestrian, Topography scenario) {
		return potentialFieldPedestrian.getRelevantAgents(relevantArea,
				pedestrian, scenario);
	}

	@Override
	public void initialize(List<Attributes> attributesList, Domain domain,
	                       AttributesAgent attributesPedestrian, Random random) {
		potentialFieldPedestrian.initialize(attributesList, domain, attributesPedestrian, random);
	}
}
