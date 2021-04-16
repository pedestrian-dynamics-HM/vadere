package org.vadere.simulator.models.sir;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.control.simulation.ControllerProvider;
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
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.shapes.*;

import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.vadere.state.attributes.Attributes.ID_NOT_SET;
import static org.vadere.state.scenario.AerosolCloud.createTransformedAerosolCloudShape;

@ModelClass
public class InfectionModel extends AbstractSirModel {


	// keep attributes here and not in AbstractSirModel becase the may change based on
	// implementation (AttributesInfectionModel is the base class for all SIR models used here for simplicity)
	private AttributesInfectionModel attributesInfectionModel;

	private ControllerProvider controllerProvider;

	double simTimeStepLength = 0.4; // ToDo how to get simTimeStepLength from simulation

	@Override
	public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
			this.domain = domain;
			this.random = random;
			this.attributesAgent = attributesPedestrian;
			this.attributesInfectionModel = Model.findAttributes(attributesList, AttributesInfectionModel.class);
	}

	@Override
	public void registerToScenarioElementControllerEvents(ControllerProvider controllerProvider) {
		this.controllerProvider = controllerProvider; // ToDo: controllerProvider should be handled by initialize method (this requires changes in all models)
		for (var controller : controllerProvider.getSourceControllers()){
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

		Collection<Pedestrian> infectedPedestrians = getInfectedPedestrians(this.domain.getTopography());
		for (Pedestrian pedestrian : infectedPedestrians) {
			if (!pedestrian.isBreathingIn() & pedestrian.getStartBreatheOutPosition() == null) {
				// start of breathing out period -> store pedestrian's position
				pedestrian.setStartBreatheOutPosition(pedestrian.getPosition());
			} else if (pedestrian.isBreathingIn() & !(pedestrian.getStartBreatheOutPosition() == null)) {
				// start of breathing in period
				// step 2: get position when pedestrian stops breathing out -> v2
				// create ellipse with vertices v1 and v2
				VPoint v1 = pedestrian.getStartBreatheOutPosition();
				pedestrian.setStartBreatheOutPosition(null); // reset startBreatheOutPosition
				VPoint v2 = pedestrian.getPosition();

				double area = Math.pow(attributesInfectionModel.getAerosolCloudInitialRadius(), 2) * Math.PI;
				VShape shape = createTransformedAerosolCloudShape(v1, v2, area);
				// ToDo find better solution to store shapeParameters
				ArrayList<VPoint> shapeParameters = new ArrayList<>();
				shapeParameters.add(0, new VPoint((v1.x + v2.x) / 2.0, (v1.y + v2.y) / 2.0));
				shapeParameters.add(1, v1);
				shapeParameters.add(2, v2);

				// assumption: aerosolCloud has a constant vertical extent (in m). The height corresponds to a
				// cylinder whose volume equals the
				// - sphere with radius = initialAerosolCloudRadius
				// - ellipsoid with principal diameters a, b, c where cross-sectional
				// area (in the x-y-plane) = a * b * PI and c = initialAerosolCloudRadius
				double height = 4.0 / 3.0 * attributesInfectionModel.getAerosolCloudInitialRadius();

				// assumption: only a part of the emitted pathogen remains in the x-y-plane
				// (at z ~ height of the pedestrians' faces) due to effects such as
				// declining "pathogen activity", evaporation, gravitation/sedimentation.
				double remainingPathogenFraction = 1.0;
				double pathogenLoad3DTo2D = (1.0 / height) * remainingPathogenFraction;
				double pathogenLoad = pedestrian.emitPathogen() / area * pathogenLoad3DTo2D;

				// assumption: pathogen load within bounds of aerosolCloud represents 99.7% of total pathogen load;
				// remainder is neglected;
				// The load inside the aerosolCloud is distributed according to a 2dimensional gaussian distribution
				// center corresponds to the mean value
				// distance between center and aerosolCloud bound represents 3 * standard deviation
				AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(ID_NOT_SET,
						shape,
						area,
						shapeParameters,
						simTimeInSec,
						attributesInfectionModel.getAerosolCloudHalfLife(),
						pathogenLoad,
						pathogenLoad,
						false));
				this.controllerProvider.registerAerosolCloud(aerosolCloud);
			}
		}

		// Agents absorb pathogen continuously but simulation is discrete. Therefore, the absorption must be adapted with normalizationFactor:
		double timeNormalizationConst = simTimeStepLength / (attributesInfectionModel.getPedestrianRespiratoryCyclePeriod() / 2);

		Collection<AerosolCloud> updatedAerosolClouds = this.domain.getTopography().getAerosolClouds();
		for (AerosolCloud aerosolCloud : updatedAerosolClouds) {
			Collection<Pedestrian> pedestriansInsideCloud = getPedestriansInsideAerosolCloud(this.domain.getTopography(), aerosolCloud);
			Collection<Pedestrian> breathingInPedestriansInsideCloud = pedestriansInsideCloud.stream().filter(p -> p.isBreathingIn()).collect(Collectors.toSet());
			for (Pedestrian pedestrian : breathingInPedestriansInsideCloud) {
				double currentMeanPathogenConcentration = aerosolCloud.getCurrentPathogenLoad() / aerosolCloud.getArea();
				double pathogenLevelAtPosition = aerosolCloud.calculatePathogenLevelAtPosition(pedestrian.getPosition());
				pedestrian.absorbPathogen(currentMeanPathogenConcentration * pathogenLevelAtPosition * timeNormalizationConst);
			}

			// Increase aerosolCloudRadius about deltaRadius due to moving agents within the cloud
			// assumption: aerosolClouds do not become greater than maxArea
			double maxArea = 10;
			if (aerosolCloud.getArea() < maxArea) {
				double deltaRadius = 0.0;
				double weight = 0.005; // each pedestrian with velocity v causes an increase of the cloud's radius by
				// factor weight * v
				for (Pedestrian pedestrian : pedestriansInsideCloud) {
					deltaRadius = deltaRadius + pedestrian.getVelocity().getLength() * weight;
				}
				aerosolCloud.increaseShape(deltaRadius);
			}
		}

		// update pedestrians
		Collection<Pedestrian> allPedestrians = this.domain.getTopography().getPedestrianDynamicElements().getElements();
		for (Pedestrian pedestrian : allPedestrians) {
			pedestrian.updateInfectionStatus(simTimeInSec);
			pedestrian.updateRespiratoryCycle(simTimeInSec, attributesInfectionModel.getPedestrianRespiratoryCyclePeriod());
		}
	}

	public static Collection<Pedestrian> getInfectedPedestrians(Topography topography) {
		return topography.getPedestrianDynamicElements()
				.getElements()
				.stream()
				.filter(p -> p.getInfectionStatus() == InfectionStatus.INFECTIOUS)
				.collect(Collectors.toSet());
	}

	public Agent sourceControllerEvent(SourceController controller, double simTimeInSec, Agent scenarioElement) {
		// SourceControllerListener. This will be called  *after* a pedestrians is inserted into the
		// topography by the given SourceController. Change model state on Agent here
		InfectionModelSourceParameters sourceParameters = defineSourceParameters(controller);

		Pedestrian ped = (Pedestrian) scenarioElement;
		ped.setInfectionStatus(sourceParameters.getInfectionStatus());
		ped.setPathogenEmissionCapacity(attributesInfectionModel.getPedestrianPathogenEmissionCapacity());
		ped.setPathogenAbsorptionRate(attributesInfectionModel.getPedestrianPathogenAbsorptionRate());
		ped.setRespiratoryTimeOffset(random.nextDouble() * attributesInfectionModel.getPedestrianRespiratoryCyclePeriod());
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
		Optional<InfectionModelSourceParameters> sourceParameters = attributesInfectionModel
				.getInfectionModelSourceParameters().stream().filter(s -> s.getSourceId() == sourceId).findFirst();

		// if sourceId not set by user, check if the user has defined default attributes by setting sourceId = -1
		if (sourceParameters.isEmpty()) {
			sourceParameters = attributesInfectionModel.getInfectionModelSourceParameters().stream().filter(s -> s.getSourceId() == defaultSourceId).findFirst();

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

	public static Collection<Pedestrian> getDynamicElementsNearAerosolCloud(Topography topography, AerosolCloud aerosolCloud) {
		final Rectangle2D aerosolCloudBounds = aerosolCloud.getShape().getBounds2D();
		final VPoint centerOfAerosolCloud = new VPoint(aerosolCloudBounds.getCenterX(), aerosolCloudBounds.getCenterY());

		final double aerosolCloudProximity = Math.max(aerosolCloudBounds.getHeight(), aerosolCloudBounds.getWidth());

		return topography.getSpatialMap(Pedestrian.class).getObjects(centerOfAerosolCloud, aerosolCloudProximity);
	}

	public static boolean isPedestrianInAerosolCloud(AerosolCloud aerosolCloud, Pedestrian pedestrian) {
		VShape aerosolCloudShape = aerosolCloud.getShape();
		VPoint pedestrianPosition = pedestrian.getPosition();
		return aerosolCloudShape.contains(pedestrianPosition);
	}

	public static Collection<Pedestrian> getPedestriansInsideAerosolCloud(Topography topography, AerosolCloud aerosolCloud) {
		Collection<Pedestrian> pedestriansInsideAerosolCloud = new LinkedList<>();

		Collection<Pedestrian> pedestriansNearAerosolCloud = getDynamicElementsNearAerosolCloud(topography, aerosolCloud);
		for (Pedestrian pedestrian : pedestriansNearAerosolCloud) {
			if (isPedestrianInAerosolCloud(aerosolCloud, pedestrian)){
				pedestriansInsideAerosolCloud.add(pedestrian);
			}
		}
		return pedestriansInsideAerosolCloud;
	}
}
