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

	private AttributesInfectionModel attributesInfectionModel;
	private ControllerProvider controllerProvider;
	double simTimeStepLength;
	Topography topography;

	@Override
	public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
			this.domain = domain;
			this.random = random;
			this.attributesAgent = attributesPedestrian;
			this.attributesInfectionModel = Model.findAttributes(attributesList, AttributesInfectionModel.class);
			this.topography = domain.getTopography();
			this.simTimeStepLength = 0.4; // ToDo how to get simTimeStepLength from simulation
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

		updateAerosolClouds(topography, attributesInfectionModel, simTimeInSec, simTimeStepLength);

		updatePedestrians(topography, attributesInfectionModel, simTimeInSec, simTimeStepLength);
	}

	public static void updateAerosolClouds(Topography topography, AttributesInfectionModel attributesInfectionModel, double simTimeInSec, double simTimeStepLength) {
		createAerosolClouds(topography, attributesInfectionModel, simTimeInSec);
		updatePathogenLoads(topography, simTimeInSec);
		updateExtents(topography, simTimeStepLength);
		deleteExpiredAerosolClouds(topography, attributesInfectionModel);
	}

	public static void updatePedestrians(Topography topography, AttributesInfectionModel attributesInfectionModel, double simTimeInSec, double simTimeStepLength) {

		// Agents absorb pathogen continuously but simulation is discrete. Therefore, the absorption must be adapted with normalizationFactor:
		double timeNormalizationConst = simTimeStepLength / (attributesInfectionModel.getPedestrianRespiratoryCyclePeriod() / 2);
		Collection<AerosolCloud> allAerosolClouds = topography.getAerosolClouds();
		for (AerosolCloud aerosolCloud : allAerosolClouds) {
			Collection<Pedestrian> pedestriansInsideCloud = getPedestriansInsideAerosolCloud(topography, aerosolCloud);
			Collection<Pedestrian> breathingInPedestriansInsideCloud = pedestriansInsideCloud.stream().filter(p -> p.isBreathingIn()).collect(Collectors.toSet());
			for (Pedestrian pedestrian : breathingInPedestriansInsideCloud) {
				double currentMeanPathogenConcentration = aerosolCloud.getCurrentPathogenLoad() / aerosolCloud.getArea();
				double pathogenLevelAtPosition = aerosolCloud.calculatePathogenLevelAtPosition(pedestrian.getPosition());
				pedestrian.absorbPathogen(currentMeanPathogenConcentration * pathogenLevelAtPosition * timeNormalizationConst);
			}
		}

		Collection<Pedestrian> allPedestrians = topography.getPedestrianDynamicElements().getElements();
		for (Pedestrian pedestrian : allPedestrians) {
			pedestrian.updateInfectionStatus(simTimeInSec);
			pedestrian.updateRespiratoryCycle(simTimeInSec, attributesInfectionModel.getPedestrianRespiratoryCyclePeriod());
		}
	}

	public static void createAerosolClouds(Topography topography, AttributesInfectionModel attributesInfectionModel, double simTimeInSec) {
		Collection<Pedestrian> infectedPedestrians = getInfectedPedestrians(topography);
		for (Pedestrian pedestrian : infectedPedestrians) {
			if (!pedestrian.isBreathingIn() & pedestrian.getStartBreatheOutPosition() == null) {
				// start of breathing out period -> store pedestrian's position -> v1
				pedestrian.setStartBreatheOutPosition(pedestrian.getPosition());
			} else if (pedestrian.isBreathingIn() & !(pedestrian.getStartBreatheOutPosition() == null)) {
				// start of breathing in period -> ped has stopped breathing out
				// step 2: get position when pedestrian stops breathing out -> v2
				// create ellipse with vertices v1 and v2
				VPoint v1 = pedestrian.getStartBreatheOutPosition();
				pedestrian.setStartBreatheOutPosition(null); // reset startBreatheOutPosition
				VPoint v2 = pedestrian.getPosition();

				double area = attributesInfectionModel.getAerosolCloudInitialArea();
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
				double radius = Math.sqrt(area / Math.PI);
				double height = 4.0 / 3.0 * radius;
				// assumption: the relevant layer (about height of the agents' heads) contains only part of the total
				// pathogenLoad and has a defined thickness
				double thicknessOfRelevantLayer = 0.3;
				double pathogenLoad = pedestrian.emitPathogen() * (thicknessOfRelevantLayer / height);

				AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(ID_NOT_SET,
						shape,
						area,
						shapeParameters,
						simTimeInSec,
						attributesInfectionModel.getAerosolCloudHalfLife(),
						pathogenLoad,
						pathogenLoad,
						false));
				topography.addAerosolCloud(aerosolCloud);
			}
		}
	}

	/**
	 * Deletes aerosolClouds that have reached less than a minimumPercentage of their initial pathogen concentration
	 * @param topography
	 */
	public static void deleteExpiredAerosolClouds(Topography topography, AttributesInfectionModel attributesInfectionModel) {
		double minimumPercentage = 0.01;
		// ToDo check if area is actual area
		Collection<AerosolCloud> aerosolCloudsToBeDeleted = topography.getAerosolClouds().stream().filter(a -> a.getCurrentPathogenLoad() / a.getArea() < minimumPercentage * a.getInitialPathogenLoad() / attributesInfectionModel.getAerosolCloudInitialArea()).collect(Collectors.toSet());
		for (AerosolCloud aerosolCloud : aerosolCloudsToBeDeleted) {
			topography.getAerosolClouds().remove(aerosolCloud);
		}
	}

	public static void updatePathogenLoads(Topography topography, double simTimeInSec) {
		Collection<AerosolCloud> allAerosolClouds = topography.getAerosolClouds();
		for (AerosolCloud aerosolCloud : allAerosolClouds) {
			double t = simTimeInSec - aerosolCloud.getCreationTime();
			double lambda = - Math.log(0.5) / aerosolCloud.getHalfLife();
			aerosolCloud.setCurrentPathogenLoad(aerosolCloud.getInitialPathogenLoad() * Math.exp(-lambda * t));
		}
	}

	public static void updateExtents(Topography topography, double simTimeStepLength) {
		Collection<AerosolCloud> allAerosolClouds = topography.getAerosolClouds();
		for (AerosolCloud aerosolCloud : allAerosolClouds) {

			// Increasing extent due to diffusion
			double rateOfSpread = 0.001;
			aerosolCloud.increaseShape(rateOfSpread * simTimeStepLength);

			// Increasing extent due to moving air caused by agents
			// Increase aerosolCloudRadius about deltaRadius due to moving agents within the cloud
			// assumption: aerosolClouds do not become greater than maxArea
			double maxArea = 10;
			Collection<Pedestrian> pedestriansInsideCloud = getPedestriansInsideAerosolCloud(topography, aerosolCloud);
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
