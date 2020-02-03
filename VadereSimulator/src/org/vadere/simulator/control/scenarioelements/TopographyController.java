package org.vadere.simulator.control.scenarioelements;

import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.Random;

public class TopographyController extends OfflineTopographyController {

	private final Topography topography;
	private final DynamicElementFactory dynamicElementFactory;

	public TopographyController(Topography topography, DynamicElementFactory dynamicElementFactory, final Random random) {
		super(topography, random);
		this.topography = topography;
		this.dynamicElementFactory = dynamicElementFactory;
	}

	public Topography getTopography() {
		return this.topography;
	}

	public void preLoop(double simTimeInSec) {
		prepareTopography();
		createAgentWrapperPedestrians();
	}

	private void createAgentWrapperPedestrians() {
		for (Pedestrian agentWrapper : topography.getInitialElements(Pedestrian.class)) {
			// Maybe, pass "attributesAgent" to "createElement()" so that attributes,
			// which are configure in GUI, are not overwritten by "createElement()" method
			Pedestrian createdPedestrian = (Pedestrian) dynamicElementFactory.createElement(agentWrapper.getPosition(),
					agentWrapper.getId(), Pedestrian.class);

			applyAttributesFromAgentWrapper(agentWrapper, createdPedestrian);
			topography.addElement(createdPedestrian);
		}
		topography.initializePedestrianCount();
	}

	private void applyAttributesFromAgentWrapper(Pedestrian agentWrapper, Pedestrian newPedestrian) {
		newPedestrian.setAttributes(agentWrapper.getAttributes());

		newPedestrian.setSource((agentWrapper.getSource()));
		newPedestrian.setTargets(agentWrapper.getTargets());
		newPedestrian.setNextTargetListIndex(agentWrapper.getNextTargetListIndex());
		newPedestrian.setIsCurrentTargetAnAgent(agentWrapper.isCurrentTargetAnAgent());

		newPedestrian.setFreeFlowSpeed(agentWrapper.getFreeFlowSpeed());
		if (!Double.isNaN(agentWrapper.getVelocity().x) && !Double.isNaN(agentWrapper.getVelocity().y)) {
			newPedestrian.setVelocity(agentWrapper.getVelocity());
		}

		newPedestrian.setFollowers(agentWrapper.getFollowers());
		newPedestrian.setIdAsTarget(agentWrapper.getIdAsTarget());
		newPedestrian.setChild(agentWrapper.isChild());
		newPedestrian.setLikelyInjured(agentWrapper.isLikelyInjured());

		newPedestrian.setMostImportantStimulus(agentWrapper.getMostImportantStimulus());
		newPedestrian.setThreatMemory(agentWrapper.getThreatMemory());
		newPedestrian.setSelfCategory(agentWrapper.getSelfCategory());
		newPedestrian.setGroupMembership(agentWrapper.getGroupMembership());

		newPedestrian.setGroupIds(agentWrapper.getGroupIds());
		newPedestrian.setGroupSizes(agentWrapper.getGroupSizes());

		agentWrapper.getTrajectory().getFootSteps().forEach(footStep -> newPedestrian.addFootStepToTrajectory(footStep));
	}

	public void update(double simTimeInSec) {
		recomputeCells();
	}

	public void postLoop(double simTimeInSec) {
		topography.reset();
	}
}
