package org.vadere.simulator.control.scenarioelements;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.scenario.*;
import org.vadere.state.scenario.distribution.DistributionFactory;
import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.awt.geom.Rectangle2D;
import java.util.*;

public class TargetController extends ScenarioElementController {

	private static final Logger log = Logger.getLogger(TargetController.class);
	private VDistribution distribution = null;
	private final AttributesTarget targetAttributes;

	public final Target target;
	private final Topography topography;


	public TargetController(Topography topography, Target target,Random random) {
		this.target = target;
		this.targetAttributes = target.getAttributes();
		this.topography = topography;

		if (targetAttributes.isWaiting()) {
			try {
				distribution = DistributionFactory.create(
						targetAttributes.getWaiterAttributes().getDistribution(),
						new JDKRandomGenerator(random.nextInt())
				);

			} catch (Exception e) {
				throw new IllegalArgumentException("Problem with scenario parameters for target: "
						+ "waitingTimeDistribution and/or distributionParameters. See causing Excepion herefafter.", e);
			}

		}
	}

	public void update(double simTimeInSec) {
		if (target.isTargetPedestrian()) {
			return;
		}

		for (DynamicElement element : getPrefilteredDynamicElements()) {
			final Agent agent = castCheckAgent(element);
			final int agentID = agent.getId();
			if(agent == null) continue;

			final boolean agentHasReachedThisTarget =
					isNextTargetForAgent(agent)
					&& hasAgentReachedThisTarget(agent);

			final boolean agentWaitingPeriodEnds =
					target.getLeavingTimes().containsKey(agentID) &&
					target.getLeavingTimes().get(agentID) <= simTimeInSec;

			if (agentHasReachedThisTarget){
				notifyListenersTargetReached(agent);
				handleArrivingAgent(agent,simTimeInSec,target.getLeavingTimes());
			}
			if (agentHasReachedThisTarget && agentWaitingPeriodEnds){
				checkRemove(agent);
			}
		}
	}

	private static Agent castCheckAgent(DynamicElement element) {
		final Agent agent;
		if (element instanceof Agent) {
			agent = (Agent) element;
		} else {
			log.error("The given object is not a subtype of Agent.");
			return null;
		}
		return agent;
	}

	private Collection<DynamicElement> getPrefilteredDynamicElements() {
		final double reachedDistance = target.getAttributes().getAbsorberAttributes().getDeletionDistance();

		final Rectangle2D bounds = target.getShape().getBounds2D();
		final VPoint center = new VPoint(bounds.getCenterX(), bounds.getCenterY());
		final double radius = Math.max(bounds.getHeight(), bounds.getWidth()) + reachedDistance;

		final Collection<DynamicElement> elementsInRange = new LinkedList<>();
		elementsInRange.addAll(getObjectsInCircle(Pedestrian.class, center, radius));
		elementsInRange.addAll(getObjectsInCircle(Car.class, center, radius));
		
		return elementsInRange;
	}

	private void waitingBehavior(final Agent agent, double simTimeInSec) {
		final int agentId = agent.getId();
		final Map<Integer, Double> leavingTimes = target.getLeavingTimes();

		final boolean agentIsWaiting = leavingTimes.containsKey(agentId);
		if (!agentIsWaiting) {
			handleArrivingAgent(agent, simTimeInSec, leavingTimes);
		}
	}

	private void handleArrivingAgent(Agent agent,double simTimeInSec, Map<Integer, Double> leavingTimes) {
		final int agentId = agent.getId();
		final int waitingSpots = target.getParallelWaiters();

		final boolean targetHasFreeWaitingSpots = waitingSpots <= 0 || (waitingSpots > 0 &&
				leavingTimes.size() < waitingSpots);
		if (targetHasFreeWaitingSpots) {
			// TODO: Refractor VadereDistributions method name
			if(targetAttributes.isWaiting()){
				leavingTimes.put(agentId,this.distribution.getNextSample(simTimeInSec));
			}else{
				leavingTimes.put(agentId,simTimeInSec);
			}
		}
	}

	private <T extends DynamicElement> List<T> getObjectsInCircle(final Class<T> clazz, final VPoint center, final double radius) {
		return topography.getSpatialMap(clazz).getObjects(center, radius);
	}

	private boolean hasAgentReachedThisTarget(Agent agent) {
		final double reachedDistance = target.getAttributes().getAbsorberAttributes().getDeletionDistance();
		final VPoint agentPosition = agent.getPosition();
		final VShape targetShape = target.getShape();

		return targetShape.contains(agentPosition)
				|| targetShape.distance(agentPosition) < reachedDistance;
	}

	private boolean isNextTargetForAgent(Agent agent) {
		boolean isNextTargetForAgent = false;

		if (agent.hasNextTarget()) {
			if (agent.getNextTargetId() == target.getId()
				&& !agent.isCurrentTargetAnAgent())
				isNextTargetForAgent = true;
		}

		return isNextTargetForAgent;
	}

	private void checkRemove(Agent agent) {
		target.getLeavingTimes().remove(agent.getId());
		if (target.isAbsorbing()) {
			changeTargetOfFollowers(agent);
			topography.removeElement(agent);
		} else {
			agent.checkNextTarget(target.getAttributes().getLeavingSpeed());
		}
	}

	private void changeTargetOfFollowers(Agent agent) {
		for (Agent follower : agent.getFollowers()) {
			follower.setSingleTarget(target.getId(), false);
		}
		agent.getFollowers().clear();
	}

	private void notifyListenersTargetReached(final Agent agent) {
		for (TargetListener l : target.getTargetListeners()) {
			l.reachedTarget(target, agent);
		}
	}
	
}
