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

	private boolean batchRemovalFinished = true;

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

		for (DynamicElement element : getNearbyPedestrians()) {
			final Agent agent = castCheckAgent(element);
			if (agent == null) continue;
			final int agentID = agent.getId();

			final boolean agentHasReachedThisTarget =
					isNextTargetForAgent(agent)
							&& hasAgentReachedThisTarget(agent);
			if (agentHasReachedThisTarget) {
				notifyListenersTargetReached(agent);
				handleArrivingAgent(agent, simTimeInSec, target.getLeavingTimes());
			}
			final boolean agentWaitingPeriodEnds =
					!targetAttributes.isWaiting() || (
							target.getLeavingTimes().containsKey(agentID) &&
									target.getLeavingTimes().get(agentID) <= simTimeInSec);
			if (agentHasReachedThisTarget && agentWaitingPeriodEnds) {
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

	private Collection<DynamicElement> getNearbyPedestrians() {
		final double reachedDistance = target.getAttributes().getAbsorberAttributes().getDeletionDistance();

		final Rectangle2D bounds = target.getShape().getBounds2D();
		final VPoint center = new VPoint(bounds.getCenterX(), bounds.getCenterY());
		final double radius = Math.max(bounds.getHeight(), bounds.getWidth()) + reachedDistance;

        return new LinkedList<>(getObjectsInCircle(Pedestrian.class, center, radius));
	}

	private void handleArrivingAgent(Agent agent, double simTimeInSec, Map<Integer, Double> leavingTimes) {
		final int agentId = agent.getId();
		final int waitingSpots = target.getParallelWaiters();

		if (targetAttributes.isWaiting()) {
			if(targetAttributes.getWaiterAttributes().isIndividualWaiting()) {
				final boolean targetHasFreeWaitingSpots = waitingSpots <= 0 || leavingTimes.size() < waitingSpots;
				if (targetHasFreeWaitingSpots) {
					if (!leavingTimes.containsKey(agentId)) {
						leavingTimes.put(agentId, this.distribution.getNextSample(simTimeInSec));
					}
				}
			}
			else {
				final boolean targetHasFreeWaitingSpots = leavingTimes.size() < waitingSpots;
				if (targetHasFreeWaitingSpots) {
					if (!leavingTimes.containsKey(agentId) && batchRemovalFinished) {
						leavingTimes.put(agentId, Double.MAX_VALUE);
						if (leavingTimes.size() == waitingSpots){
							double nextSample = this.distribution.getNextSample(simTimeInSec);
							for (Map.Entry<Integer, Double> entry : leavingTimes.entrySet()) {
								entry.setValue(nextSample);
							}
						}
					}
				}
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
		if (targetAttributes.isWaiting()) {
			target.getLeavingTimes().remove(agent.getId());
			if(!targetAttributes.getWaiterAttributes().isIndividualWaiting()){
				batchRemovalFinished = false;
				if (target.getLeavingTimes().isEmpty()){
					batchRemovalFinished = true;
				}
			}
		}
		if (target.isAbsorbing()) {
			changeTargetOfFollowers(agent);
			topography.removeElement(agent);
		} else {
			agent.checkNextTarget();

			// set a new desired speed, if possible. you can model street networks with differing
			// maximal speeds with this.
			double nextSpeed = target.getAttributes().getLeavingSpeed();
			if (nextSpeed >= 0) {
				agent.setFreeFlowSpeed(nextSpeed);
			}

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
