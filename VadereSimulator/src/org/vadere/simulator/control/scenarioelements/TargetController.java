package org.vadere.simulator.control.scenarioelements;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.vadere.state.attributes.scenario.AttributesTarget;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Car;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.TargetListener;
import org.vadere.state.scenario.Topography;
import org.vadere.state.scenario.distribution.DistributionFactory;

import org.vadere.state.scenario.distribution.VDistribution;
import org.vadere.state.types.TrafficLightPhase;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.awt.geom.Rectangle2D;
import java.util.*;

public class TargetController extends ScenarioElementController {


	private final Target.WaitingBehaviour waitingBehaviour;
	private static final Logger log = Logger.getLogger(TargetController.class);
	private VDistribution distribution = null;
	private final AttributesTarget targetAttributes;

	private double phaseLength = 0;

	public final Target target;
	private Topography topography;

	public TrafficLightPhase phase;

	public TargetController(Topography topography, Target target,Random random) {
		this.target = target;
		this.targetAttributes = target.getAttributes();
		this.topography = topography;

		if (this.target.isStartingWithRedLight()) {
			phase = TrafficLightPhase.RED;
		} else {
			phase = TrafficLightPhase.GREEN;
		}
		if (!targetAttributes.getWaitingBehaviour().equals(Target.WaitingBehaviour.NO_WAITING)) {
			try {
				distribution = DistributionFactory.create(
						targetAttributes.getWaitingTimeDistribution(),
						targetAttributes.getDistributionParameters(),
						0,
						new JDKRandomGenerator(random.nextInt())
				);

			} catch (Exception e) {
				throw new IllegalArgumentException("Problem with scenario parameters for target: "
						+ "waitingTimeDistribution and/or distributionParameters. See causing Excepion herefafter.", e);
			}

			this.phaseLength = this.distribution.getNextSpawnTime(0);
		}
		this.waitingBehaviour = target.getWaitingBehaviour();
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
			}
			if (agentHasReachedThisTarget && agentWaitingPeriodEnds){
				checkRemove(agent);
			}
			if (agentHasReachedThisTarget && !agentWaitingPeriodEnds){
				waitingBehavior(agent, simTimeInSec);
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
		final double reachedDistance = target.getAttributes().getDeletionDistance();

		final Rectangle2D bounds = target.getShape().getBounds2D();
		final VPoint center = new VPoint(bounds.getCenterX(), bounds.getCenterY());
		final double radius = Math.max(bounds.getHeight(), bounds.getWidth()) + reachedDistance;

		final Collection<DynamicElement> elementsInRange = new LinkedList<>();
		elementsInRange.addAll(getObjectsInCircle(Pedestrian.class, center, radius));
		elementsInRange.addAll(getObjectsInCircle(Car.class, center, radius));
		
		return elementsInRange;
	}

	private void waitingBehavior(final Agent agent, double simTimeInSec) {
		// individual waiting behaviour, as opposed to waiting at a traffic light
		switch (waitingBehaviour){
			case INDIVIDUAL: {
				waitIndividually(agent, simTimeInSec);
				break;
			}
			case TRAFFIC_LIGHT: {
				waitTrafficLight(agent,simTimeInSec);
				break;
			}
			case NO_WAITING:{
				checkRemove(agent);
				break;
			}
		}
	}

	private void waitTrafficLight(Agent agent, double simTimeInSec) {
		// traffic light switching based on waiting time. Light starts green.
		phase = getCurrentTrafficLightPhase(simTimeInSec);
		if (phase == TrafficLightPhase.GREEN) {
			checkRemove(agent);
		}
	}

	private void waitIndividually(Agent agent, double simTimeInSec) {
		final int agentId = agent.getId();
		final Map<Integer, Double> leavingTimes = target.getLeavingTimes();

		final boolean agentIsWaiting = leavingTimes.containsKey(agentId);
		if (agentIsWaiting) {
			handleWaitingAgent(agent, simTimeInSec, leavingTimes);
		} else {
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
			leavingTimes.put(agentId, this.distribution.getNextSpawnTime(simTimeInSec));
		}
	}

	private void handleWaitingAgent(Agent agent, double simTimeInSec, Map<Integer, Double> leavingTimes) {
		final int agentId = agent.getId();
		final boolean agentWaitingTimeIsOver = simTimeInSec > leavingTimes.get(agentId);

		if (agentWaitingTimeIsOver) {
			leavingTimes.remove(agentId);
			checkRemove(agent);
		}
	}

	private <T extends DynamicElement> List<T> getObjectsInCircle(final Class<T> clazz, final VPoint center, final double radius) {
		return topography.getSpatialMap(clazz).getObjects(center, radius);
	}

	private boolean hasAgentReachedThisTarget(Agent agent) {
		final double reachedDistance = target.getAttributes().getDeletionDistance();
		final VPoint agentPosition = agent.getPosition();
		final VShape targetShape = target.getShape();

		return targetShape.contains(agentPosition)
				|| targetShape.distance(agentPosition) < reachedDistance;
	}

	private TrafficLightPhase getCurrentTrafficLightPhase(double simTimeInSec) {
		double phaseSecond = simTimeInSec % (this.phaseLength * 2 + target.getWaitingTimeYellowPhase() * 2);

		if (target.isStartingWithRedLight()) {
			if (phaseSecond < this.phaseLength)
				return TrafficLightPhase.RED;
			if (phaseSecond < this.phaseLength + target.getWaitingTimeYellowPhase())
				return TrafficLightPhase.YELLOW;
			if (phaseSecond < this.phaseLength * 2 + target.getWaitingTimeYellowPhase())
				return TrafficLightPhase.GREEN;

		} else {
			if (phaseSecond < this.phaseLength)
				return TrafficLightPhase.GREEN;
			if (phaseSecond < this.phaseLength + target.getWaitingTimeYellowPhase())
				return TrafficLightPhase.YELLOW;
			if (phaseSecond < this.phaseLength * 2 + target.getWaitingTimeYellowPhase())
				return TrafficLightPhase.RED;

		}
		return TrafficLightPhase.YELLOW;
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
		if (target.isAbsorbing()) {
			changeTargetOfFollowers(agent);
			topography.removeElement(agent);
		} else {
			agent.checkNextTarget(target.getNextSpeed());
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
