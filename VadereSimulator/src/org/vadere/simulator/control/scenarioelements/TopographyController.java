package org.vadere.simulator.control.scenarioelements;

import org.vadere.simulator.control.scenarioelements.listener.ControllerEventListener;
import org.vadere.simulator.control.scenarioelements.listener.ControllerEventProvider;
import org.vadere.simulator.models.DynamicElementFactory;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.exceptions.AttributesNotFoundException;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TopographyController extends OfflineTopographyController implements ControllerEventProvider<Agent, TopographyController> {

	private static Logger logger = Logger.getLogger(TopographyController.class);
	private final Domain domain;
	private final DynamicElementFactory dynamicElementFactory;

	protected ArrayList<ControllerEventListener<Agent, TopographyController>> eventListeners = new ArrayList<>();

	public TopographyController(Domain domain, DynamicElementFactory dynamicElementFactory, final Random random) {
		super(domain, random);
		this.domain = domain;
		this.dynamicElementFactory = dynamicElementFactory;
	}

	public Topography getTopography() {
		return this.domain.getTopography();
	}

	public void preLoop(double simTimeInSec, List<Attributes> attributesList) {
		// If there is no background mesh these attributes are used to constrcut a distance function by using a cellgrid of size defined by AttributesFloorField.
		AttributesFloorField attributesFloorField;
		try {
			attributesFloorField = Model.findAttributes(attributesList, AttributesFloorField.class);
		} catch (AttributesNotFoundException ex) {
			logger.warn("no " + AttributesFloorField.class.getName() + " found, the default values are used instead.");
			// if there is none use the default values.
			attributesFloorField = new AttributesFloorField();
		}
		prepareTopography(attributesFloorField);
		createAgentWrapperPedestrians(simTimeInSec);
	}

	private void createAgentWrapperPedestrians(double simTimeInSec) {
		for (Pedestrian agentWrapper : domain.getTopography().getInitialElements(Pedestrian.class)) {
			// Maybe, pass "attributesAgent" to "createElement()" so that attributes,
			// which are configure in GUI, are not overwritten by "createElement()" method
			Pedestrian createdPedestrian = (Pedestrian) dynamicElementFactory.createElement(agentWrapper.getPosition(),
					agentWrapper.getId(), Pedestrian.class);

			applyAttributesFromAgentWrapper(agentWrapper, createdPedestrian);

			for (ControllerEventListener<Agent, TopographyController> listener : eventListeners) {
				createdPedestrian = (Pedestrian) listener.notify(this, simTimeInSec, createdPedestrian);
			}

			domain.getTopography().addElement(createdPedestrian);
		}
		domain.getTopography().initializePedestrianCount();
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

		newPedestrian.setHealthStatus(agentWrapper.getHealthStatus());
	}

	public void update(double simTimeInSec) {
		recomputeCells();
	}

	public void postLoop(double simTimeInSec) {
		domain.getTopography().reset();
	}

	@Override
	public void register(ControllerEventListener<Agent, TopographyController> listener) {
		if (! eventListeners.contains(listener)){
			eventListeners.add(listener);
		}
	}

	@Override
	public void unregister(ControllerEventListener<Agent, TopographyController> listener) {
		eventListeners.remove(listener);
	}
}
