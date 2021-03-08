package org.vadere.simulator.models.sir;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.control.simulation.ControllerManager;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributeSIR;
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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.vadere.state.attributes.Attributes.ID_NOT_SET;

@ModelClass
public class InfectionModel extends AbstractSirModel {


	// keep attributes here and not in AbstractSirModel becase the may change based on
	// implementation (AttributeSIR is the base class for all SIR models used here for simplicity)
	private AttributeSIR attributeSIR;

	private double someModelState;

	private int counter;

	@Override
	public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
			this.domain = domain;
			this.random = random;
			this.attributesAgent = attributesPedestrian;
			this.attributeSIR = Model.findAttributes(attributesList, AttributeSIR.class);
			this.counter = 0;

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
	public void preLoop(double simTimeInSec) { logger.infof(">>>>>>>>>>>DummySirModel preLoop %f", simTimeInSec); }

	@Override
	public void postLoop(double simTimeInSec) {
		logger.infof(">>>>>>>>>>>DummySirModel postLoop %f", simTimeInSec);
	}

	@Override
	public void update(double simTimeInSec) {
		logger.infof(">>>>>>>>>>>DummySirModel update  %f", simTimeInSec);
		// do your model code here....
		this.someModelState = simTimeInSec * this.attributeSIR.getInitialR();

		// just for testing
		if (counter < 1) {
			VPoint position = new VPoint(28, 6);
			AerosolCloud newAerosolCloud = new AerosolCloud(new AttributesAerosolCloud(ID_NOT_SET, (VShape) new VCircle(position, 0.75), simTimeInSec, 1000000, 60 * 15, false));
			this.domain.getTopography().addAerosolCloud(newAerosolCloud);
			counter += 1;
		}

		if (this.attributeSIR.getInfectionModelLastUpdateTime() < 0 || simTimeInSec >= this.attributeSIR.getInfectionModelLastUpdateTime() + this.attributeSIR.getInfectionModelUpdateStepLength()) {
			this.attributeSIR.setInfectionModelLastUpdateTime(simTimeInSec);

			// add new cloud for each infectious pedestrian at current position and simTime
			Collection<Pedestrian> infectedPedestrians = this.domain.getTopography().getPedestrianDynamicElements()
					.getElements()
					.stream()
					.filter(p -> p.getInfectionStatus() == InfectionStatus.INFECTIOUS)
					.collect(Collectors.toSet());
			for (Pedestrian pedestrian : infectedPedestrians) {
				AerosolCloud newAerosolCloud = new AerosolCloud(new AttributesAerosolCloud(ID_NOT_SET, (VShape) new VCircle(pedestrian.getPosition(), 0.75), simTimeInSec, pedestrian.getPathogenEmissionCapacity(), 60 * 15, false));
				this.domain.getTopography().addAerosolCloud(newAerosolCloud);
			}

			// delete old aerosolClouds
			Collection<AerosolCloud> aerosolClouds = this.domain.getTopography().getAerosolClouds().stream().filter(a -> a.getHasReachedLifeEnd()).collect(Collectors.toSet());
			for (AerosolCloud aerosolCloud : aerosolClouds) {
				this.domain.getTopography().getAerosolClouds().remove(aerosolCloud);
			}

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
		}
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

	public Collection<Pedestrian> getDynamicElementsNearAerosolCloud(AerosolCloud aerosolCloud) {
		final Rectangle2D aerosolCloudBounds = aerosolCloud.getShape().getBounds2D();
		final VPoint centerOfAerosolCloud = new VPoint(aerosolCloudBounds.getCenterX(), aerosolCloudBounds.getCenterY());

		final double aerosolCloudProximity = Math.max(aerosolCloudBounds.getHeight(), aerosolCloudBounds.getWidth());

		List<Pedestrian> pedestriansNearAerosolCloud = this.domain.getTopography().getSpatialMap(Pedestrian.class).getObjects(centerOfAerosolCloud, aerosolCloudProximity);

		return pedestriansNearAerosolCloud;
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
