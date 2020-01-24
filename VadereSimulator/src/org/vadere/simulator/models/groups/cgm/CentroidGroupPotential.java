package org.vadere.simulator.models.groups.cgm;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesCGM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.logging.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Random;

@ModelClass
public class CentroidGroupPotential implements PotentialFieldAgent {

	private static Logger logger = Logger.getLogger(CentroidGroupPotential.class);

	private final AttributesCGM attributesCGM;
	private final CentroidGroupModel groupCollection;
	private final PotentialFieldAgent potentialFieldPedestrian;
	private final IPotentialFieldTarget potentialFieldTarget;

	public CentroidGroupPotential(CentroidGroupModel groupCollection,
								  PotentialFieldAgent pedestrianRepulsionPotential,
								  IPotentialFieldTarget potentialFieldTarget,
								  AttributesCGM attributesCGM) {

		this.attributesCGM = attributesCGM;
		this.groupCollection = groupCollection;
		this.potentialFieldPedestrian = pedestrianRepulsionPotential;
		this.potentialFieldTarget = potentialFieldTarget;
	}

	@Override
	public double getAgentPotential(IPoint pos, Agent pedestrian,
	                                Collection<? extends Agent> closePedestrians) {
		double result = 0;

		if (!(pedestrian instanceof Pedestrian))
			throw new IllegalArgumentException("Centroid group can only handle type Pedestrian");

		Pedestrian ped = (Pedestrian) pedestrian;

		result += getPedestrianGroupPotential(ped, pos);
		result += getPedestrianRepulsionPotential(ped, pos,
				closePedestrians);

		return result;
	}

	private double getPedestrianGroupPotential(Pedestrian ped, IPoint pos) {
		double result = 0;
		CentroidGroup group = groupCollection.getGroup(ped);
		Pedestrian leader = null;

		if (group != null && group.getSize() > 1) {
			leader = group.getPacemaker(ped);
		}

		if (leader != null) {
			VPoint leaderPoint = leader.getPosition();
			double leaderPotential = potentialFieldTarget.getPotential(leaderPoint, leader);
			double pedPotential = potentialFieldTarget.getPotential(ped.getPosition(), ped);

			final double[] distanceToCentroid = {
					pos.getX() - leaderPoint.getX(),
					pos.getY() - leaderPoint.getY()};

			result = attributesCGM.getLeaderAttractionFactor()
					* Math.pow(
					leaderPotential - pedPotential,
					4);
		}

		return result;
	}

	private double getPedestrianRepulsionPotential(Pedestrian ped, IPoint pos,
												   Collection<? extends Agent> closePedestrians) {
		double potential = 0;

		for (Agent neighborBody : closePedestrians) {
			if (neighborBody != ped) {
				potential += getAgentPotential(pos, ped, neighborBody);
			}
		}

		return potential;
	}

	@Override
	public Vector2D getAgentPotentialGradient(IPoint pos,
											  Vector2D velocity, Agent pedestrian,
											  Collection<? extends Agent> closePedestrians) {
		// TODO [priority=low] [task=refactoring] not implemented
		throw new UnsupportedOperationException("this method is not jet implemented.");
		// return new Vector2D(0, 0);
	}

	@Override
	public double getAgentPotential(IPoint pos, Agent pedestrian,
									Agent otherPedestrian) {
		//System.out.printf("Ped1: %s, Ped1: %s %n", pedestrian.getId(), otherPedestrian.getId());
		CentroidGroup group = groupCollection.getGroup((Pedestrian)pedestrian);
		CentroidGroup groupOther = groupCollection.getGroup((Pedestrian)otherPedestrian);
		double potential = potentialFieldPedestrian.getAgentPotential(pos,
				pedestrian, otherPedestrian);

		if (group != null && group.equals(groupOther)) {
			potential *= attributesCGM.getGroupMemberRepulsionFactor();
		}

		return potential;
	}

	@Override
	public Collection<? extends Agent> getRelevantAgents(VCircle relevantArea,
														 Agent pedestrian, Topography scenario) {
		return potentialFieldPedestrian.getRelevantAgents(relevantArea,
				pedestrian, scenario);
	}

	@Override
	public void initialize(List<Attributes> attributesList, Domain topography,
	                       AttributesAgent attributesPedestrian, Random random) {
		// TODO [priority=medium] [task=refactoring] should be used to initialize the Model
	}

}
