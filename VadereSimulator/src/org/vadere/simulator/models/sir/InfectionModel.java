package org.vadere.simulator.models.sir;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.control.simulation.ControllerManager;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesInfectionModel;
import org.vadere.state.attributes.models.InfectionModelSourceParameters;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.health.InfectionStatus;
import org.vadere.state.scenario.AerosolCloud;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;

import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.stream.Collectors;

import static org.vadere.state.attributes.Attributes.ID_NOT_SET;

@ModelClass
public class InfectionModel extends AbstractSirModel {


	// keep attributes here and not in AbstractSirModel becase the may change based on
	// implementation (AttributesInfectionModel is the base class for all SIR models used here for simplicity)
	private AttributesInfectionModel attributesInfectionModel;

	private double someModelState;

	private int counter;

	private ControllerManager controllerManager;

	@Override
	public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
			this.domain = domain;
			this.random = random;
			this.attributesAgent = attributesPedestrian;
			this.attributesInfectionModel = Model.findAttributes(attributesList, AttributesInfectionModel.class);
			this.counter = 0;
	}

	@Override
	public void registerToScenarioElementControllerEvents(ControllerManager controllerManager) {
		this.controllerManager = controllerManager; // ToDo: controllerManager should be handled by initialize method (this requires changes in all models)
		for (var controller : controllerManager.getSourceControllers()){
			controller.register(this::sourceControllerEvent);
		}
	}

	@Override
	public void preLoop(double simTimeInSec) { logger.infof(">>>>>>>>>>>InfectionModelModel preLoop %f", simTimeInSec); }

	@Override
	public void postLoop(double simTimeInSec) {
		logger.infof(">>>>>>>>>>>InfectionModelModel postLoop %f", simTimeInSec);
	}

	@Override
	public void update(double simTimeInSec) {
		logger.infof(">>>>>>>>>>>InfectionModelModel update  %f", simTimeInSec);

		if (this.attributesInfectionModel.getInfectionModelLastUpdateTime() < 0 || simTimeInSec >= this.attributesInfectionModel.getInfectionModelLastUpdateTime() + this.attributesInfectionModel.getInfectionModelUpdateStepLength()) {
			this.attributesInfectionModel.setInfectionModelLastUpdateTime(simTimeInSec);

			// add new cloud for each infectious pedestrian at current position and simTime
			Collection<Pedestrian> infectedPedestrians = this.domain.getTopography().getPedestrianDynamicElements()
					.getElements()
					.stream()
					.filter(p -> p.getInfectionStatus() == InfectionStatus.INFECTIOUS)
					.collect(Collectors.toSet());
			for (Pedestrian pedestrian : infectedPedestrians) {
				AerosolCloud newAerosolCloud = new AerosolCloud(new AttributesAerosolCloud(ID_NOT_SET,
						new VCircle(pedestrian.getPosition(), attributesInfectionModel.getAerosolCloudInitialRadius()),
						simTimeInSec,
						pedestrian.emitPathogen(),
						attributesInfectionModel.getAerosolCloudLifeTime(),
						false));
				// add newAerosolCloud and aerosolCloudController for that cloud to topography:
				this.controllerManager.registerAerosolCloud(newAerosolCloud);
			}

			// update absorbed pathogen load for each pedestrian and each cloud (every x-th loop):
			Collection<AerosolCloud> updatedAerosolClouds = this.domain.getTopography().getAerosolClouds();
			for (AerosolCloud aerosolCloud : updatedAerosolClouds) {
				Collection<Pedestrian> pedestriansInsideCloud = getPedestriansInsideAerosolCloud(aerosolCloud);
				for (Pedestrian pedestrian : pedestriansInsideCloud) {
					updatePedestrianPathogenAbsorbedLoad(pedestrian, aerosolCloud.getPathogenLoad());

				}
			}

			// add model for droplet infection
			// ...

			// update pedestrian infection statuses
			Collection<Pedestrian> allPedestrians = this.domain.getTopography().getPedestrianDynamicElements().getElements();
			for (Pedestrian pedestrian : allPedestrians) {
				updatePedestrianInfectionStatus(pedestrian, simTimeInSec);
			}
		}
	}

	public Agent sourceControllerEvent(SourceController controller, double simTimeInSec, Agent scenarioElement) {
		// SourceControllerListener. This will be called  *after* a pedestrians is inserted into the
		// topography by the given SourceController. Change model state on Agent here
		InfectionModelSourceParameters sourceParameters = defineSourceParameters(controller);

		Pedestrian ped = (Pedestrian) scenarioElement;
		ped.setInfectionStatus(sourceParameters.getInfectionStatus());
		ped.setPathogenEmissionCapacity(attributesInfectionModel.getPedestrianPathogenEmissionCapacity());
		ped.setPathogenAbsorptionRate(attributesInfectionModel.getPedestrianPathogenAbsorptionRate());
		ped.setSusceptibility(attributesInfectionModel.getPedestrianSusceptibility());
		ped.setExposedPeriod(attributesInfectionModel.getExposedPeriod());
		ped.setInfectiousPeriod(attributesInfectionModel.getInfectiousPeriod());
		ped.setRecoveredPeriod(attributesInfectionModel.getRecoveredPeriod());

		logger.infof(">>>>>>>>>>>sourceControllerEvent at time: %f  agentId: %d", simTimeInSec, scenarioElement.getId());
		return ped;
	}

	private InfectionModelSourceParameters defineSourceParameters(SourceController controller) {
		int sourceId = controller.getSourceId();
		int defaultSourceId = -1;
		Optional<InfectionModelSourceParameters> sourceParameters = getAttributesInfectionModel()
				.getInfectionModelSourceParameters().stream().filter(s -> s.getSourceId() == sourceId).findFirst();

		// if sourceId not set by user, check if the user has defined default attributes by setting sourceId = -1
		if (sourceParameters.isEmpty()) {
			sourceParameters = getAttributesInfectionModel().getInfectionModelSourceParameters().stream().filter(s -> s.getSourceId() == defaultSourceId).findFirst();

			// if no user defined default values: use attributesInfectionModel default values
			if (sourceParameters.isPresent()) {
				logger.infof(">>>>>>>>>>>defineSourceParameters: sourceId %d not set explicitly in infectionModelSourceParameters. Source uses default infectionModelSourceParameters defined for sourceId: %d", sourceId, defaultSourceId);
			} else {
				logger.errorf(">>>>>>>>>>>defineSourceParameters: sourceId %d is not set in infectionModelSourceParameters", sourceId);
			}
		}
			return sourceParameters.get();
	}

	public AttributesInfectionModel getAttributesInfectionModel() {
		return attributesInfectionModel;
	}

	public Collection<Pedestrian> getDynamicElementsNearAerosolCloud(AerosolCloud aerosolCloud) {
		final Rectangle2D aerosolCloudBounds = aerosolCloud.getShape().getBounds2D();
		final VPoint centerOfAerosolCloud = new VPoint(aerosolCloudBounds.getCenterX(), aerosolCloudBounds.getCenterY());

		final double aerosolCloudProximity = Math.max(aerosolCloudBounds.getHeight(), aerosolCloudBounds.getWidth());

		return this.domain.getTopography().getSpatialMap(Pedestrian.class).getObjects(centerOfAerosolCloud, aerosolCloudProximity);
	}

	public boolean isPedestrianInAerosolCloud(AerosolCloud aerosolCloud, Pedestrian pedestrian) {
		VShape aerosolCloudShape = aerosolCloud.getShape();
		VPoint pedestrianPosition = pedestrian.getPosition();
		return aerosolCloudShape.contains(pedestrianPosition);
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
