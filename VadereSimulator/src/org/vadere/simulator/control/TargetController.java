package org.vadere.simulator.control;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Car;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.TrafficLightPhase;
import org.vadere.util.geometry.shapes.VPoint;

public class TargetController {

	private static final Logger log = Logger.getLogger(TargetController.class);

	public final Target target;
	private Topography topography;

	public TrafficLightPhase phase = TrafficLightPhase.GREEN;

	public TargetController(Topography scenario, Target target) {
		this.target = target;
		this.topography = scenario;

		if (this.target.isStartingWithRedLight()) {
			phase = TrafficLightPhase.RED;
		} else {
			phase = TrafficLightPhase.GREEN;
		}
	}

	public void update(double simTimeInSec) {
		if (this.target.isTargetPedestrian()) {
			return;
		}
		final double reachedDistance = target.getAttributes().getDeletionDistance();

		Rectangle2D bounds = target.getShape().getBounds2D();
		VPoint center = new VPoint(bounds.getCenterX(), bounds.getCenterY());
		double radius = Math.max(bounds.getHeight(), bounds.getWidth()) + reachedDistance;

		Collection<DynamicElement> elementsInRange = new LinkedList<>();
		elementsInRange.addAll(topography.getSpatialMap(Pedestrian.class).getObjects(center, radius));
		elementsInRange.addAll(topography.getSpatialMap(Car.class).getObjects(center, radius));


		for (DynamicElement element : elementsInRange) {

			final Agent agent;
			if (element instanceof Agent) {
				agent = (Agent) element;
			} else {
				log.error("The given object is not a subtype of Agent.");
				continue;
			}

			if (isNextTargetForAgent(agent)
					&& hasAgentReachedThisTarget(agent, reachedDistance)) {

				if (target.getWaitingTime() <= 0) {
					checkRemove(agent);
				} else {
					final int agentId = agent.getId();
					// individual waiting behaviour, as opposed to waiting at a traffic light
					if (target.getAttributes().isIndividualWaiting()) {
						final Map<Integer, Double> enteringTimes = target.getEnteringTimes();
						if (enteringTimes.containsKey(agentId)) {
							if (simTimeInSec - enteringTimes.get(agentId) > target
									.getWaitingTime()) {
								enteringTimes.remove(agentId);
								checkRemove(agent);
							}
						} else {
							final int parallelWaiters = target.getParallelWaiters();
							if (parallelWaiters <= 0 || (parallelWaiters > 0 &&
									enteringTimes.size() < parallelWaiters)) {
								enteringTimes.put(agentId, simTimeInSec);
							}
						}
					} else {
						// traffic light switching based on waiting time. Light starts green.
						phase = getCurrentTrafficLightPhase(simTimeInSec);

						if (phase == TrafficLightPhase.GREEN) {
							checkRemove(agent);
						}
					}
				}
			}
		}
	}

	private boolean hasAgentReachedThisTarget(Agent agent, double reachedDistance) {
		return target.getShape().contains(agent.getPosition())
				|| target.getShape().distance(agent.getPosition()) < reachedDistance;
	}

	private TrafficLightPhase getCurrentTrafficLightPhase(double simTimeInSec) {
		double phaseSecond = simTimeInSec % (target.getWaitingTime() * 2 + target.getWaitingTimeYellowPhase() * 2);

		if (target.isStartingWithRedLight()) {
			if (phaseSecond < target.getWaitingTime())
				return TrafficLightPhase.RED;
			if (phaseSecond < target.getWaitingTime() + target.getWaitingTimeYellowPhase())
				return TrafficLightPhase.YELLOW;
			if (phaseSecond < target.getWaitingTime() * 2 + target.getWaitingTimeYellowPhase())
				return TrafficLightPhase.GREEN;

			return TrafficLightPhase.YELLOW;
		} else {
			if (phaseSecond < target.getWaitingTime())
				return TrafficLightPhase.GREEN;
			if (phaseSecond < target.getWaitingTime() + target.getWaitingTimeYellowPhase())
				return TrafficLightPhase.YELLOW;
			if (phaseSecond < target.getWaitingTime() * 2 + target.getWaitingTimeYellowPhase())
				return TrafficLightPhase.RED;

			return TrafficLightPhase.YELLOW;
		}
	}

	private boolean isNextTargetForAgent(Agent agent) {
		if (agent.hasNextTarget()) {
			return agent.getNextTargetId() == target.getId();
		}
		return false;
	}

	// TODO [priority=high] [task=deprecation] removing the target from the list is deprecated, but still very frequently used everywhere.
	private void checkRemove(Agent agent) {
		if (target.isAbsorbing()) {
			topography.removeElement(agent);
		} else {
			// Deprecated target list usage
			if (agent.getNextTargetListIndex() == -1 && !agent.getTargets().isEmpty()) {
				agent.getTargets().removeFirst();
			}

			// The right way (later this first check should not be necessary anymore):
			if (agent.getNextTargetListIndex() != -1) {
				if (agent.getNextTargetListIndex() < agent.getTargets().size()) {
					agent.incrementNextTargetListIndex();
				}
			}

			// set a new desired speed, if possible. you can model street networks with differing
			// maximal speeds with this.
			if (target.getNextSpeed() >= 0) {
				agent.setFreeFlowSpeed(target.getNextSpeed());
			}
		}
	}
}
