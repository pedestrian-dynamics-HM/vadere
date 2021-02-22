package org.vadere.simulator.models.sir;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.control.simulation.ControllerManager;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributeSIR;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;

import java.util.List;
import java.util.Random;

@ModelClass
public class DummySirModel extends AbstractSirModel {



	// keep attributes here and not in AbstractSirModel becase the may change based on
	// implementation (AttributeSIR is the base class for all SIR models used here for simplicity)
	private AttributeSIR attributeSIR;

	private double someModelState;

	@Override
	public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
			this.domain = domain;
			this.random = random;
			this.attributesAgent = attributesPedestrian;
			this.attributeSIR = Model.findAttributes(attributesList, AttributeSIR.class);

			// initialize modelState
			someModelState = 1 * attributeSIR.getInitialR();
	}

	@Override
	public void registerToScenarioElementControllerEvents(ControllerManager controllerManager) {
		for (var controller : controllerManager.getSourceControllers()){
			controller.register(this::sourceControllerEvent);
		}
	}

	@Override
	public void preLoop(double simTimeInSec) {
		logger.infof(">>>>>>>>>>>DummySirModel preLoop %f", simTimeInSec);
	}

	@Override
	public void postLoop(double simTimeInSec) {
		logger.infof(">>>>>>>>>>>DummySirModel postLoop %f", simTimeInSec);
	}

	@Override
	public void update(double simTimeInSec) {
		logger.infof(">>>>>>>>>>>DummySirModel update  %f", simTimeInSec);
		// do your model code here....
		this.someModelState = simTimeInSec * this.attributeSIR.getInitialR();

		// access topography
//		 Topography topography = this.domain.getTopography();

		// access measurement areas (static none moving areas for infection?)
//		 MeasurementArea area = this.domain.getTopography().getMeasurementArea(attributeSIR.getInfectionZoneIds().get(0));

		// access all Pedestrians
//		this.domain.getTopography().getPedestrianDynamicElements().getElements().stream()....
	}

	public Agent sourceControllerEvent(SourceController controller, double simTimeInSec, Agent scenarioElement) {
		// SourceControllerListener. This will be called  *after* a pedestrians is inserted into the
		// topography by the given SourceController. Change model state on Agent here
		logger.infof(">>>>>>>>>>>sourceControllerEvent at time: %f  agentId: %d", simTimeInSec, scenarioElement.getId());
		return scenarioElement;
	}

	public AttributeSIR getAttributeSIR() {
		return attributeSIR;
	}


	public double getSomeModelState() {
		return someModelState;
	}

}
