package org.vadere.simulator.models.sir;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.context.VadereContext;
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
import org.vadere.state.health.*;

/**
 * This class models the spread of infectious pathogen among pedestrians.
 * For this purpose, the InfectionModel controls the airborne transmission of pathogen from infectious pedestrians to
 * other pedestrians, i.e. it
 * <ul>
 *     <li>initializes each pedestrian's {@link HealthStatus} after a pedestrian is inserted into the topography,</li>
 *     <li>updates the pedestrian's {@link HealthStatus}</li>
 *     <li>creates, updates and deletes each {@link AerosolCloud}</li>
 * </ul>
 */
@ModelClass
public class InfectionModel extends AbstractSirModel {

	private AttributesInfectionModel attributesInfectionModel;
	double simTimeStepLength;
	Topography topography;

	public static final String simStepLength = "simTimeStepLength";

	/*
	 * constant that results from exponential decay of pathogen concentration: C(t) = C_init * exp(-lambda * t),
	 * lambda = exponentialDecayFactor / halfLife
	 */
	private static final double exponentialDecayFactor = Math.log(2.0);

	/* minimumPercentage defines a percentage of the initial pathogen concentration
	 * (pathogenLoad / aerosolCloud.volume); As soon as an aerosolCloud has reached the minimum concentration, the
	 * aerosolCloud is considered negligible and therefore deleted
	 */
	private static final double minimumPercentage = 0.01;

	/* rateOfSpread describes how fast the aerosolCloud spreads due to diffusion; unit: m/s; and
	 * could be implemented as user-defined parameter in AttributesInfectionModel
	 */
	private static final double rateOfSpread = 0.001;

	/* each pedestrian with velocity v causes an increase of the cloud's radius by factor
	 * weight * v * simTimeStepLength; could be implemented as user-defined parameter in AttributesInfectionModel
	 */
	private static final double weight = 0.0125;

	@Override
	public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
			this.domain = domain;
			this.random = random;
			this.attributesAgent = attributesPedestrian;
			this.attributesInfectionModel = Model.findAttributes(attributesList, AttributesInfectionModel.class);
			this.topography = domain.getTopography();
			this.simTimeStepLength = VadereContext.get(this.topography).getDouble(simStepLength);
	}

	@Override
	public void registerToScenarioElementControllerEvents(ControllerProvider controllerProvider) {
		// ToDo: controllerProvider should be handled by initialize method (this requires changes in all models)
		for (var controller : controllerProvider.getSourceControllers()){
			controller.register(this::sourceControllerEvent);
		}
	}

	@Override
	public void preLoop(double simTimeInSec) {}

	@Override
	public void postLoop(double simTimeInSec) {}

	@Override
	public void update(double simTimeInSec) {
		logger.infof(">>>>>>>>>>>InfectionModelModel update  %f", simTimeInSec);

		updateAerosolClouds(simTimeInSec);
		updatePedestrians(simTimeInSec);
	}

	public void updateAerosolClouds(double simTimeInSec) {
		createAerosolClouds(simTimeInSec);
		updateAerosolCloudsPathogenLoad(simTimeInSec);
		updateAerosolCloudsExtent();
		deleteExpiredAerosolClouds();
	}

	public void createAerosolClouds(double simTimeInSec) {
		Collection<Pedestrian> infectedPedestrians = getInfectedPedestrians(topography);
		for (Pedestrian pedestrian : infectedPedestrians) {
			if (pedestrian.isStartingBreatheOut()) {
				pedestrian.setStartBreatheOutPosition(pedestrian.getPosition());

			} else if (pedestrian.isStartingBreatheIn()) {
				VPoint startBreatheOutPosition = pedestrian.getStartBreatheOutPosition();
				VPoint stopBreatheOutPosition = pedestrian.getPosition();

				AerosolCloud aerosolCloud = generateAerosolCloud(simTimeInSec, pedestrian, startBreatheOutPosition, stopBreatheOutPosition);
				topography.addAerosolCloud(aerosolCloud);

				// reset pedestrian's startBreatheOutPosition
				pedestrian.setStartBreatheOutPosition(null);
			}
		}
	}

	private AerosolCloud generateAerosolCloud(double simTimeInSec, Pedestrian pedestrian, VPoint v1, VPoint v2) {
		double initialArea = attributesInfectionModel.getAerosolCloudInitialArea();
		VShape shape = createTransformedAerosolCloudShape(v1, v2, initialArea);
		ArrayList<VPoint> vertices = new ArrayList<>(Arrays.asList(v1, v2));
		VPoint center = new VPoint((v1.x + v2.x) / 2.0, (v1.y + v2.y) / 2.0);

		/* Assumption: The aerosolCloud has a constant vertical extent (in m). The height corresponds to a
		 * cylinder whose volume cylinderVolume equals the volume of a sphere with radius = sqrt(initialArea / PI)
		 * => volumeSphere = 4 / 3 * PI * radius^3 and baseArea = radius^2 * PI
		 *
		 * cylinderBaseArea = baseArea
		 * height = cylinderHeight = cylinderVolume / cylinderBaseArea =
		 * = sphereVolume / baseArea = (4 / 3 * PI * radius^3) / (radius^2 * PI) = 4 / 3 * radius
		 */
		double radius = Math.sqrt(initialArea / Math.PI);
		double height = 4.0 / 3.0 * radius;

		AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(ID_NOT_SET,
				shape,
				initialArea,
				height,
				center,
				vertices,
				simTimeInSec,
				attributesInfectionModel.getAerosolCloudHalfLife(),
				pedestrian.emitPathogen(),
				pedestrian.emitPathogen()));

		return aerosolCloud;
	}

	public void updateAerosolCloudsPathogenLoad(double simTimeInSec) {
		Collection<AerosolCloud> allAerosolClouds = topography.getAerosolClouds();
		for (AerosolCloud aerosolCloud : allAerosolClouds) {
			double t = simTimeInSec - aerosolCloud.getCreationTime();
			double lambda = exponentialDecayFactor / aerosolCloud.getHalfLife();
			aerosolCloud.setCurrentPathogenLoad(aerosolCloud.getInitialPathogenLoad() * Math.exp(-lambda * t));
		}
	}

	public void updateAerosolCloudsExtent() {
		Collection<AerosolCloud> allAerosolClouds = topography.getAerosolClouds();
		for (AerosolCloud aerosolCloud : allAerosolClouds) {

			// Increasing extent due to diffusion
			aerosolCloud.increaseShape(rateOfSpread * simTimeStepLength);

			// Increasing extent due to moving air caused by agents
			// Increase aerosolCloudRadius about deltaRadius due to moving agents within the cloud
			Collection<Pedestrian> pedestriansInsideCloud = getPedestriansInsideAerosolCloud(topography, aerosolCloud);
			double deltaRadius = 0.0;
			for (Pedestrian pedestrian : pedestriansInsideCloud) {
				deltaRadius = deltaRadius + pedestrian.getVelocity().getLength() * weight * simTimeStepLength;
			}
			aerosolCloud.increaseShape(deltaRadius);
		}
	}

	public void deleteExpiredAerosolClouds() {
		Collection<AerosolCloud> aerosolCloudsToBeDeleted = topography.getAerosolClouds()
				.stream()
				.filter(a -> a.getCurrentPathogenLoad() / a.getArea() < minimumPercentage * a.getInitialPathogenLoad() / attributesInfectionModel.getAerosolCloudInitialArea())
				.collect(Collectors.toSet());
		for (AerosolCloud aerosolCloud : aerosolCloudsToBeDeleted) {
			topography.getAerosolClouds().remove(aerosolCloud);
		}
	}

	public void updatePedestrians(double simTimeInSec) {

		updatePedPathogenLoad();

		Collection<Pedestrian> allPedestrians = topography.getPedestrianDynamicElements().getElements();
		for (Pedestrian pedestrian : allPedestrians) {
			pedestrian.updateInfectionStatus(simTimeInSec);
			pedestrian.updateRespiratoryCycle(simTimeInSec, attributesInfectionModel.getPedestrianRespiratoryCyclePeriod());
		}
	}

	private void updatePedPathogenLoad() {
		// Agents absorb pathogen continuously but simulation is discrete. Therefore, the absorption must be adapted with normalizationFactor:
		double timeNormalizationConst = simTimeStepLength / (attributesInfectionModel.getPedestrianRespiratoryCyclePeriod() / 2.0);
		Collection<AerosolCloud> allAerosolClouds = topography.getAerosolClouds();
		for (AerosolCloud aerosolCloud : allAerosolClouds) {
			Collection<Pedestrian> breathingInPedsInsideCloud = getPedestriansInsideAerosolCloud(topography, aerosolCloud)
					.stream()
					.filter(Pedestrian::isBreathingIn)
					.collect(Collectors.toSet());

			for (Pedestrian pedestrian : breathingInPedsInsideCloud) {
				double volume = aerosolCloud.getHeigth() * aerosolCloud.getArea();
				double meanPathogenConcentration = aerosolCloud.getCurrentPathogenLoad() / volume;
				// assumption: the pathogen is distributed uniformly within the aerosolCloud
				// alternatively, calculate the level according to a gaussian distribution with	and multiply with the
				// meanPathogenConcentration
				// double pathogenLevelAtPosition = aerosolCloud.calculatePathogenLevelAtPosition(pedestrian.getPosition());

				pedestrian.absorbPathogen(meanPathogenConcentration * timeNormalizationConst);
			}
		}
	}

	public Collection<Pedestrian> getInfectedPedestrians(Topography topography) {
		return topography.getPedestrianDynamicElements()
				.getElements()
				.stream()
				.filter(p -> p.getInfectionStatus() == InfectionStatus.INFECTIOUS)
				.collect(Collectors.toSet());
	}

	public Agent sourceControllerEvent(SourceController controller, double simTimeInSec, Agent scenarioElement) {
		// SourceControllerListener. This will be called  *after* a pedestrian is inserted into the
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
