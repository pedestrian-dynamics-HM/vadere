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
import org.vadere.state.attributes.scenario.AttributesDroplets;
import org.vadere.state.health.InfectionStatus;
import org.vadere.state.scenario.*;
import org.vadere.util.geometry.shapes.*;

import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.vadere.state.scenario.AerosolCloud.createTransformedAerosolCloudShape;
import static org.vadere.state.scenario.Droplets.createTransformedDropletsShape;
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
	int aerosolCloudIdCounter;

	private Map<Integer, VPoint> lastPedestrianPositions;
	private Map<Integer, Vector2D> viewingDirections;
	private static final double MIN_STEP_LENGTH = 0.1;

	/**
	 * Key that is used for initializeVadereContext in ScenarioRun
	 */
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
			this.aerosolCloudIdCounter = 1;
			this.viewingDirections = new HashMap<>();
			this.lastPedestrianPositions = new HashMap<>();
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

		executePathogenEmissionEvents(simTimeInSec);
		updateAerosolClouds(simTimeInSec);
		updateDroplets(simTimeInSec);
		updatePedestrians(simTimeInSec);
	}

	public void executePathogenEmissionEvents(double simTimeInSec) {
		Collection<Pedestrian> infectedPedestrians = getInfectedPedestrians(topography);
		for (Pedestrian pedestrian : infectedPedestrians) {
			// ... for each user-defined event
			createAerosolClouds(simTimeInSec, pedestrian);
			createDroplets(simTimeInSec, pedestrian);
		}
	}

	public void updateAerosolClouds(double simTimeInSec) {
		updateAerosolCloudsPathogenLoad(simTimeInSec);
		updateAerosolCloudsExtent();
		deleteExpiredAerosolClouds();
	}

	public void updateDroplets(double simTimeInSec) {
		// dropletsPathogenLoad remains unchanged until deletion
		deleteExpiredDroplets(simTimeInSec);
	}

	public void createAerosolClouds(double simTimeInSec, Pedestrian pedestrian) {

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

		AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(aerosolCloudIdCounter,
				shape,
				initialArea,
				height,
				center,
				vertices,
				simTimeInSec,
				attributesInfectionModel.getAerosolCloudHalfLife(),
				pedestrian.emitPathogen(),
				pedestrian.emitPathogen()));

		aerosolCloudIdCounter = aerosolCloudIdCounter + 1;

		return aerosolCloud;
	}

	private void createDroplets(double simTimeInSec, Pedestrian pedestrian) {
		/*
		 * Parameters for JSON // ToDo integrate these parameters in attributesInfectionModel
		 */
		double dropletExhalationFrequency = 1 / 30.0; // droplets are exhaled approximately every 30 sec
		double distanceOfSpread = 1.5;
		double angleOfSpreadInRad = Math.toRadians(30.0);
		double lifeTime = 1.0 + 1.0E-3; // make sure that lifeTime is not a multiple of simTimeStepLength
		double emittedPathogenLoad = pedestrian.getSusceptibility() / attributesInfectionModel.getPedestrianPathogenAbsorptionRate();

		// ToDo: remove this quick solution; it would be better to have the walking directions stored in pedestrian
		int pedestrianId = pedestrian.getId();
		Vector2D viewingDirection;
		VPoint currentPosition = pedestrian.getPosition();
		VPoint lastPosition = lastPedestrianPositions.get(pedestrianId);
		if (lastPedestrianPositions.get(pedestrianId) == null) {
			viewingDirection = new Vector2D(Math.random(), Math.random());
		} else {
			if (lastPosition.distance(currentPosition) < MIN_STEP_LENGTH) {
				viewingDirection = viewingDirections.get(pedestrianId);
			} else {
				viewingDirection = new Vector2D(currentPosition.getX() - lastPosition.getX(),
						currentPosition.getY() - lastPosition.getY());
			}
		}
		viewingDirection.normalize(1);
		viewingDirections.put(pedestrianId, viewingDirection);
		lastPedestrianPositions.put(pedestrianId, currentPosition);

		if (simTimeInSec % (1 / dropletExhalationFrequency) <= simTimeStepLength) {

			VShape shape = createTransformedDropletsShape(pedestrian.getPosition(), viewingDirection, distanceOfSpread, angleOfSpreadInRad);

			Droplets droplets = new Droplets(new AttributesDroplets(1,
					shape,
					simTimeInSec,
					lifeTime,
					emittedPathogenLoad));

			topography.addDroplets(droplets);
		}
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
			// ToDo: to be discussed if it makes sense to use the agent's velocity at each simStep (shouldn't it be the
			//  mean velocity within each simStep? or is that too detailed?)
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

	public void deleteExpiredDroplets(double simTimeInSec) {
		Collection<Droplets> dropletsToBeDeleted = topography.getDroplets()
				.stream()
				.filter(d -> d.getLifeTime() + d.getCreationTime() < simTimeInSec)
				.collect(Collectors.toSet());
		for (Droplets droplets : dropletsToBeDeleted) {
			topography.getDroplets().remove(droplets);
		}
	}

	public void updatePedestrians(double simTimeInSec) {
		updatePedsPathogenLoad();
		updatePedsHealthStatus(simTimeInSec);
	}

	private void updatePedsHealthStatus(double simTimeInSec) {
		Collection<Pedestrian> allPedestrians = topography.getPedestrianDynamicElements().getElements();
		for (Pedestrian pedestrian : allPedestrians) {
			pedestrian.updateInfectionStatus(simTimeInSec);
			pedestrian.updateRespiratoryCycle(simTimeInSec, attributesInfectionModel.getPedestrianRespiratoryCyclePeriod());
		}
	}

	private void updatePedsPathogenLoad() {
		Collection<Pedestrian> breathingInPeds = topography.getPedestrianDynamicElements()
				.getElements()
				.stream()
				.filter(Pedestrian::isBreathingIn)
				.collect(Collectors.toSet());

		pathogenFromAerosolClouds(breathingInPeds);

		pathogenFromDroplets(breathingInPeds);
	}

	private void pathogenFromAerosolClouds(Collection<Pedestrian> breathingInPeds) {
		// Agents absorb pathogen continuously but simulation is discrete. Therefore, the absorption must be adapted with normalizationFactor:
		double timeNormalizationConst = simTimeStepLength / (attributesInfectionModel.getPedestrianRespiratoryCyclePeriod() / 2.0);
		Collection<AerosolCloud> allAerosolClouds = topography.getAerosolClouds();
		for (AerosolCloud aerosolCloud : allAerosolClouds) {
			Collection<Pedestrian> breathingInPedsInAerosolCloud = breathingInPeds
					.stream()
					.filter(p -> aerosolCloud.getShape().contains(p.getPosition()))
					.collect(Collectors.toSet());

			for (Pedestrian ped : breathingInPedsInAerosolCloud) {
				double volume = aerosolCloud.getHeight() * aerosolCloud.getArea();
				double meanPathogenConcentration = aerosolCloud.getCurrentPathogenLoad() / volume;
				// assumption: the pathogen is distributed uniformly within the aerosolCloud
				// alternatively, calculate the level according to a gaussian distribution with	and multiply with the
				// meanPathogenConcentration
				// double pathogenLevelAtPosition = aerosolCloud.calculatePathogenLevelAtPosition(pedestrian.getPosition());
				ped.absorbPathogen(meanPathogenConcentration * timeNormalizationConst);
			}
		}
	}

	private void pathogenFromDroplets(Collection<Pedestrian> breathingInPeds) {
		Collection<Droplets> allDroplets = topography.getDroplets();
		for (Droplets droplets : allDroplets) {
			Collection<Pedestrian> breathingInPedsInDroplets = breathingInPeds
					.stream()
					.filter(p -> droplets.getShape().contains(p.getPosition()))
					.collect(Collectors.toSet());
			for (Pedestrian ped : breathingInPedsInDroplets) {
				ped.absorbPathogen(droplets.getCurrentPathogenLoad());
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
