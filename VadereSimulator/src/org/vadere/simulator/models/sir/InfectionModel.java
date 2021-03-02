package org.vadere.simulator.models.sir;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.control.simulation.ControllerManager;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributeSIR;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.health.InfectionStatus;
import org.vadere.state.scenario.AerosolCloud;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@ModelClass
public class InfectionModel extends AbstractSirModel {



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

		// event queue: every x-th loop:
		// 		update aerosol clouds -> AerosolCloudController
		// 		add new clouds -> AerosolCloudController


		// update absorbed pathogen load for each pedestrian and each cloud (every x-th loop):
		Collection<AerosolCloud> updatedAerosolClouds = this.domain.getTopography().getAerosolClouds(); // ToDo check Topography: more changes required?
		for (AerosolCloud aerosolCloud : updatedAerosolClouds) {
			Collection<Pedestrian> pedestriansInsideCloud = getPedestriansInsideAerosolCloud(aerosolCloud);
			for (Pedestrian pedestrian : pedestriansInsideCloud) {
				updatePedestrianPathogenAbsorbedLoad(pedestrian, aerosolCloud.getPathogenLoad());


			}
		}

		// update pedestrian infection statuses
		Collection<Pedestrian> allPedestrians = this.domain.getTopography().getPedestrianDynamicElements().getElements();
		for (Pedestrian pedestrian : allPedestrians) {
			updatePedestrianInfectionStatus(pedestrian, simTimeInSec);
		}

		// call AerosolCloudController ? -> update aerosol clouds (add new ones, modify, delete old clouds)

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


	// ToDo: move method to SirModel.java (how to use "domain" in SirModel.java?)
	public Collection<Pedestrian> getInfectedPedestrians(){
		if (domain == null) return new LinkedList<>();
		return this.domain.getTopography().getPedestrianDynamicElements().getElements()
				.stream()
				.filter(p -> p.getInfectionStatus() == InfectionStatus.INFECTIOUS)
				.collect(Collectors.toList());
	}

	public Collection<Pedestrian> getDynamicElementsNearAerosolCloud(AerosolCloud aerosolCloud) {
		final Rectangle2D aerosolCloudBounds = aerosolCloud.getShape().getBounds2D();
		final VPoint centerOfAerosolCloud = new VPoint(aerosolCloudBounds.getCenterX(), aerosolCloudBounds.getCenterY());

		final double aerosolCloudRadius = aerosolCloud.getRadius();
		final double aerosolCloudProximity = Math.max(aerosolCloudBounds.getHeight(), aerosolCloudBounds.getWidth()) + aerosolCloudRadius;

		List<Pedestrian> pedestriansNearAerosolCloud = this.domain.getTopography().getSpatialMap(Pedestrian.class).getObjects(centerOfAerosolCloud, aerosolCloudProximity);

		return pedestriansNearAerosolCloud;
	}

	public boolean isPedestrianInAerosolCloud(AerosolCloud aerosolCloud, Pedestrian pedestrian) {
		final double aerosolCloudRadius = aerosolCloud.getRadius();
		final VPoint pedestrianPosition = pedestrian.getPosition();
		final VCircle aerosolCloudShape = aerosolCloud.getShape();

		return aerosolCloudShape.contains(pedestrianPosition) || aerosolCloudShape.distance(pedestrianPosition) < aerosolCloudRadius;
	}

	public Collection<Pedestrian> getPedestriansInsideAerosolCloud(AerosolCloud aerosolCloud) {
		Collection<Pedestrian> pedestriansInsideAerosolCloud = new LinkedList<>();

		Collection<Pedestrian> pedestriansNearAerosolCloud = getDynamicElementsNearAerosolCloud(aerosolCloud);
		for (Pedestrian pedestrian : pedestriansNearAerosolCloud) {
			if (isPedestrianInAerosolCloud(aerosolCloud, pedestrian)){
				pedestriansInsideAerosolCloud.add(pedestrian);
			}
		}
		return pedestriansInsideAerosolCloud;
	}
}
