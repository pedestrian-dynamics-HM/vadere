package org.vadere.simulator.models.sir;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.control.simulation.ControllerProvider;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributeSIR;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.medicine.InfectionStatus;
import org.vadere.state.scenario.*;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;


import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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
	public void registerToScenarioElementControllerEvents(ControllerProvider controllerProvider) {
		for (var controller : controllerProvider.getSourceControllers()){
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

		// update aerosol clouds
		Collection<AerosolCloud> allAerosolClouds = this.domain.getTopography().getAerosolClouds();
		for (AerosolCloud aerosolCloud : allAerosolClouds) {
			// TODO reduce pathogen load
			// TODO increase radius
			deleteAerosolCloud(aerosolCloud, simTimeInSec);
		}

		// add new clouds every update loop (aim is a defined frequency, set-off should be individual for each pedestrian)
		Collection<Pedestrian> infectedPedestrians = getInfectedPedestrians();
		for (Pedestrian pedestrian : infectedPedestrians) {
			createAerosolCloud(pedestrian.getPosition(), simTimeInSec);
		}

		// update absorbed pathogen load for each pedestrian and each cloud (aim is a defined frequency, set-off should be individual for each pedestrian)
		Collection<AerosolCloud> updatedAerosolClouds = this.domain.getTopography().getAerosolClouds();
		for (AerosolCloud aerosolCloud : updatedAerosolClouds) {
			// how to use simulator.scenarioelements.AerosolCloudController? -> get all pedestrians inside a cloud; is this another option?
			Collection<Pedestrian> pedestriansInsideCloud = getPedestriansInsideAerosolCloud(aerosolCloud);
			for (Pedestrian pedestrian : pedestriansInsideCloud) {
				absorbPathogenFromAerosolCloud(pedestrian, aerosolCloud.getAerosolCloudPathogenLoad());
			}
		}

		// update pedestrian infection statuses
		Collection<Pedestrian> allPedestrians = this.domain.getTopography().getPedestrianDynamicElements().getElements();
		for (Pedestrian pedestrian : allPedestrians) {
			updateInfectionStatus(pedestrian, simTimeInSec);
		}


		// access topography
//		 Topography topography = this.domain.getTopography();

		// access measurement areas (static none moving areas for infection?)
//		 MeasurementArea area = this.domain.getTopography().getMeasurementArea(attributeSIR.getInfectionZoneIds().get(0));

		// access all Pedestrians
//		this.domain.getTopography().getPedestrianDynamicElements().getElements().stream()....
	}

	public void createAerosolCloud(Pedestrian pedestrian, double simTimeInSec) {
		// TODO create cloud (how?) BK / SS
		VPoint position = pedestrian.getPosition();
		double pathogenLoad = pedestrian.getPathogenEmissionCapacity();
		AerosolCloud aerosolCloud = new AerosolCloud();

		aerosolCloud.setAerosolPersistenceStart(simTimeInSec);
		aerosolCloud.setAerosolCloudPathogenLoad(pathogenLoad);
		// set shape (circle), center, radius

		this.domain.getTopography().addAerosolCloud(aerosolCloud);
	}

	public void deleteAerosolCloud(AerosolCloud aerosolCloud, double simTimeInSec) {
		if (simTimeInSec > aerosolCloud.getAerosolPersistenceStart() + aerosolCloud.getAerosolPersistenceTime()) {
			// TODO: delete aerosol cloud (how?) BK / SS
			this.domain.getTopography().removeElement(aerosolCloud);
		} else {
			// do nothing
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

	public Collection<Pedestrian> getInfectedPedestrians(){
		if (domain == null) return new LinkedList<>();
		return this.domain.getTopography().getPedestrianDynamicElements().getElements()
				.stream()
				.filter(p -> p.getInfectionStatus() == InfectionStatus.INFECTIOUS)
				.collect(Collectors.toList());
	}


	private Collection<Pedestrian> getDynamicElementsNearAerosolCloud(AerosolCloud aerosolCloud) {
		final Rectangle2D aerosolCloudBounds = aerosolCloud.getShape().getBounds2D();
		final VPoint centerOfAerosolCloud = new VPoint(aerosolCloudBounds.getCenterX(), aerosolCloudBounds.getCenterY());

		final double aerosolCloudRadius = aerosolCloud.getAerosolCloudRadius();
		final double aerosolCloudProximity = Math.max(aerosolCloudBounds.getHeight(), aerosolCloudBounds.getWidth()) + aerosolCloudRadius;

		List<Pedestrian> pedestriansNearAerosolCloud = this.domain.getTopography().getSpatialMap(Pedestrian.class).getObjects(centerOfAerosolCloud, aerosolCloudProximity);

		return pedestriansNearAerosolCloud;
	}

	private boolean isPedestrianInAerosolCloud(Pedestrian pedestrian, AerosolCloud aerosolCloud) {
		final double aerosolCloudRadius = aerosolCloud.getAerosolCloudRadius();
		final VPoint pedestrianPosition = pedestrian.getPosition();
		final VShape aerosolCloudShape = aerosolCloud.getShape();

		return aerosolCloudShape.contains(pedestrianPosition) || aerosolCloudShape.distance(pedestrianPosition) < aerosolCloudRadius;
	}

	public Collection<Pedestrian> getPedestriansInsideAerosolCloud(AerosolCloud aerosolCloud) {
		Collection<Pedestrian> pedestriansInsideAerosolCloud = new LinkedList<>();

		Collection<Pedestrian> pedestriansNearAerosolCloud = getDynamicElementsNearAerosolCloud(aerosolCloud);
		for (Pedestrian pedestrian : pedestriansNearAerosolCloud) {
			if (isPedestrianInAerosolCloud(pedestrian, aerosolCloud)){
				pedestriansInsideAerosolCloud.add(pedestrian);
			}
		}
		return pedestriansInsideAerosolCloud;
	}

	public void absorbPathogenFromAerosolCloud(Pedestrian pedestrian, double pathogenLoad) {
		InfectionStatus infectionStatus = pedestrian.getInfectionStatus();
		double absorbedPathogen = pedestrian.getPathogenAbsorptionRate() * pathogenLoad;
		double accumulatedAbsorbedAbsorbedPathogen = pedestrian.getAbsorbedAmountOfPathogen() + absorbedPathogen;

		switch (infectionStatus) {
			case SUSCEPTIBLE:
				pedestrian.setAbsorbedAmountOfPathogen(accumulatedAbsorbedAbsorbedPathogen);
				break;
			case EXPOSED:
				pedestrian.setAbsorbedAmountOfPathogen(accumulatedAbsorbedAbsorbedPathogen);
				break;
			case INFECTIOUS:
			case RECOVERED:
				break; // do not absorb
			default:
				throw new IllegalStateException("Unexpected value: " + infectionStatus);
		}
	}

	public void updateInfectionStatus(Pedestrian pedestrian, double simTimeInSec) {
		InfectionStatus infectionStatus = pedestrian.getInfectionStatus();
		switch (infectionStatus) {
			case SUSCEPTIBLE:
				if (pedestrian.getAbsorbedAmountOfPathogen() >= pedestrian.getSusceptibility()) {
					pedestrian.setInfectionStatus(InfectionStatus.EXPOSED);
					pedestrian.setExposedStartTime(simTimeInSec);
				}
				break;
			case EXPOSED:
				if (simTimeInSec >= pedestrian.getExposedStartTime() + pedestrian.getExposedPeriod()) {
					pedestrian.setInfectionStatus(InfectionStatus.INFECTIOUS);
					pedestrian.setInfectiousStartTime(simTimeInSec);
				}
				break;
			case INFECTIOUS:
				if (simTimeInSec >= pedestrian.getInfectiousStartTime() + pedestrian.getInfectiousPeriod()) {
					pedestrian.setInfectionStatus(InfectionStatus.RECOVERED);
					pedestrian.setRecoveredStartTime(simTimeInSec);
					pedestrian.setAbsorbedAmountOfPathogen(0.0);
				}
				break;
			case RECOVERED:
				if (simTimeInSec >= pedestrian.getRecoveredStartTime() + pedestrian.getRecoveredPeriod()) {
					pedestrian.setInfectionStatus(InfectionStatus.SUSCEPTIBLE);
				}
				break;
		}
	}
}
