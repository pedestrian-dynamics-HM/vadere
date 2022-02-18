package org.vadere.simulator.models.infection;

import org.lwjgl.system.CallbackI;
import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.control.simulation.ControllerProvider;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesTransmissionModel;
import org.vadere.state.attributes.models.TransmissionModelSourceParameters;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesDroplets;
import org.vadere.state.health.HealthStatus;
import org.vadere.state.health.InfectionStatus;
import org.vadere.state.health.TransmissionModelHealthStatus;
import org.vadere.state.scenario.*;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.logging.Logger;

import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.stream.Collectors;

import static org.vadere.state.scenario.Droplets.createTransformedDropletsShape;

/**
 * This class models the spread of infectious pathogen among pedestrians.
 * For this purpose, the TransmissionModel controls the airborne transmission of pathogen from infectious pedestrians to
 * other pedestrians, i.e. it
 * <ul>
 *     <li>initializes each pedestrian's {@link HealthStatus} after a pedestrian is inserted into the topography,</li>
 *     <li>updates the pedestrian's {@link HealthStatus}</li>
 *     <li>creates, updates and deletes each {@link AerosolCloud}</li>
 *     <li>creates, updates and deletes {@link Droplets}</li>
 * </ul>
 */
@ModelClass
public class TransmissionModel extends AbstractExposureModel {

	protected static Logger logger = Logger.getLogger(TransmissionModel.class);

	private AttributesTransmissionModel attributesTransmissionModel;
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
	 * could be implemented as user-defined parameter in AttributesTransmissionModel
	 */
	private static final double rateOfSpread = 0.001;

	/* each pedestrian with velocity v causes an increase of the cloud's radius by factor
	 * weight * v * simTimeStepLength; could be implemented as user-defined parameter in AttributesTransmissionModel
	 */
	private static final double weight = 0.0125;

	@Override
	public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
			this.domain = domain;
			this.random = random;
			this.attributesAgent = attributesPedestrian;
			this.attributesTransmissionModel = Model.findAttributes(attributesList, AttributesTransmissionModel.class);
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
		//ToDo: move boolean isXyModelDefined to initialize method and distinguish between:
		// * HealthStatus
		// * AerosolCloudModel
		// * AlternativeAerosolCloudModel (not yet implemented)
		// * DropletModel
		// * ... (any other transmission route)
		// Consider this also in AttributesTransmissionModel

		//ToDo: alternatively, introduce sub-submodels and iterate through sub-submodels, similarly to loop over all
		// models in class Simulation

		boolean isAerosolCloudModelDefined = attributesTransmissionModel.getAerosolCloudHalfLife() > 0;
		boolean isDropletModelDefined = attributesTransmissionModel.getDropletsExhalationFrequency() > 0;

		// this model for transmission via aerosol clouds is enabled by default
		if (isAerosolCloudModelDefined) {
			executeAerosolCloudEmissionEvents(simTimeInSec);
			updateAerosolClouds(simTimeInSec);
			updatePedsPathogenLoadFromAerosolClouds();
		}

		// this model for transmission via droplets is disabled by default
		if (isDropletModelDefined) {
			executeDropletEmissionEvents(simTimeInSec);
			updateDroplets(simTimeInSec);
			updatePedsPathogenLoadFromDroplets();
		}

		if (isAerosolCloudModelDefined || isDropletModelDefined) {
			updatePedsHealthStatus(simTimeInSec);
		}
	}

	@Override
	public void updatePedestrianDegreeOfExposure(Pedestrian pedestrian, double degreeOfExposure) {
		pedestrian.absorbPathogen(degreeOfExposure);
	}

	public void executeAerosolCloudEmissionEvents(double simTimeInSec) {
		Collection<Pedestrian> infectedPedestrians = getInfectedPedestrians(topography);
		for (Pedestrian pedestrian : infectedPedestrians) {
			// ... for each user-defined event
			createAerosolClouds(simTimeInSec, pedestrian);
		}
	}

	public void executeDropletEmissionEvents(double simTimeInSec) {
		Collection<Pedestrian> infectedPedestrians = getInfectedPedestrians(topography);
		for (Pedestrian pedestrian : infectedPedestrians) {
			// ... for each user-defined event
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
			VLine distanceWalkedDuringExhalation = new VLine(startBreatheOutPosition, stopBreatheOutPosition);

			AerosolCloud aerosolCloud = generateAerosolCloud(simTimeInSec, pedestrian, distanceWalkedDuringExhalation);
			topography.addAerosolCloud(aerosolCloud);

			// reset pedestrian's startBreatheOutPosition
			pedestrian.setStartBreatheOutPosition(null);
		}
	}

	private AerosolCloud generateAerosolCloud(double simTimeInSec, Pedestrian pedestrian, VLine distanceWalkedDuringExhalation) {
		VPoint center = distanceWalkedDuringExhalation.midPoint();

		double radius = attributesTransmissionModel.getAerosolCloudInitialRadius();

		AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(aerosolCloudIdCounter,
				radius,
				center,
				simTimeInSec,
				attributesTransmissionModel.getAerosolCloudHalfLife(),
				pedestrian.emitPathogen(),
				pedestrian.emitPathogen()));

		aerosolCloudIdCounter = aerosolCloudIdCounter + 1;

		return aerosolCloud;
	}

	private void createDroplets(double simTimeInSec, Pedestrian pedestrian) {

		if (attributesTransmissionModel.getDropletsExhalationFrequency() > 0) {

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

			// period between two droplet generating respiratory events
			double dropletExhalationPeriod = 1 / attributesTransmissionModel.getDropletsExhalationFrequency();

			if (simTimeInSec % dropletExhalationPeriod < simTimeStepLength) {

				VShape shape = createTransformedDropletsShape(pedestrian.getPosition(),
						viewingDirection,
						attributesTransmissionModel.getDropletsDistanceOfSpread(),
						Math.toRadians(attributesTransmissionModel.getDropletsAngleOfSpreadInDeg()));

				double emittedPathogenLoad = pedestrian.emitPathogen() * attributesTransmissionModel.getDropletsPathogenLoadFactor();

				Droplets droplets = new Droplets(new AttributesDroplets(1,
						shape,
						simTimeInSec,
						attributesTransmissionModel.getDropletsLifeTime(),
						emittedPathogenLoad));

				topography.addDroplets(droplets);
			}
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
			// aerosolCloud.increaseShape(rateOfSpread * simTimeStepLength);

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

	/*
	 * Deletes aerosolClouds with negligible pathogenConcentration, that is if current pathogen concentration is smaller
	 * than a threshold (minimumPercentage * initial pathogen concentration)
	 */
	public void deleteExpiredAerosolClouds() {
		double initialRadius = attributesTransmissionModel.getAerosolCloudInitialRadius();

		Collection<AerosolCloud> aerosolCloudsToBeDeleted = topography.getAerosolClouds()
				.stream()
				.filter(a -> a.getPathogenConcentration() < minimumPercentage * a.getInitialPathogenLoad() / AerosolCloud.radiusToVolume(initialRadius))
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

	private void updatePedsHealthStatus(double simTimeInSec) {
		Collection<Pedestrian> allPedestrians = topography.getPedestrianDynamicElements().getElements();
		for (Pedestrian pedestrian : allPedestrians) {
			pedestrian.updateInfectionStatus(simTimeInSec);
			pedestrian.updateRespiratoryCycle(simTimeInSec, attributesTransmissionModel.getPedestrianRespiratoryCyclePeriod());
		}
	}

	private void updatePedsPathogenLoadFromAerosolClouds() {
		Collection<Pedestrian> breathingInPeds = topography.getPedestrianDynamicElements()
				.getElements()
				.stream()
				.filter(Pedestrian::isBreathingIn)
				.collect(Collectors.toSet());

		// Agents absorb pathogen continuously but simulation is discrete. Therefore, the absorption must be adapted with normalizationFactor:
		double timeNormalizationConst = simTimeStepLength / (attributesTransmissionModel.getPedestrianRespiratoryCyclePeriod() / 2.0);
		Collection<AerosolCloud> allAerosolClouds = topography.getAerosolClouds();
		for (AerosolCloud aerosolCloud : allAerosolClouds) {
			Collection<Pedestrian> breathingInPedsInAerosolCloud = breathingInPeds
					.stream()
					.filter(p -> aerosolCloud.getShape().contains(p.getPosition()))
					.collect(Collectors.toSet());

			for (Pedestrian ped : breathingInPedsInAerosolCloud) {
				updatePedestrianDegreeOfExposure(ped, aerosolCloud.getPathogenConcentration() * timeNormalizationConst);
			}
		}
	}

	private void updatePedsPathogenLoadFromDroplets() {
		Collection<Pedestrian> breathingInPeds = topography.getPedestrianDynamicElements()
				.getElements()
				.stream()
				.filter(Pedestrian::isBreathingIn)
				.collect(Collectors.toSet());

		Collection<Droplets> allDroplets = topography.getDroplets();
		for (Droplets droplets : allDroplets) {
			Collection<Pedestrian> breathingInPedsInDroplets = breathingInPeds
					.stream()
					.filter(p -> droplets.getShape().contains(p.getPosition()))
					.collect(Collectors.toSet());
			for (Pedestrian ped : breathingInPedsInDroplets) {
				updatePedestrianDegreeOfExposure(ped, droplets.getCurrentPathogenLoad());
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
		TransmissionModelSourceParameters sourceParameters = defineSourceParameters(controller);

		Pedestrian ped = (Pedestrian) scenarioElement;
		ped.addHealthStatus(TransmissionModelHealthStatus.class);
		ped.setInfectious(sourceParameters.isInfectious());
		ped.setDegreeOfExposure(0);

		//TODO cast healthStatus in method getHealthStatus()
		((TransmissionModelHealthStatus)ped.getHealthStatus()).setRespiratoryTimeOffset(random.nextDouble() * attributesTransmissionModel.getPedestrianRespiratoryCyclePeriod());
		((TransmissionModelHealthStatus)ped.getHealthStatus()).setBreathingIn(false);
		//TODO check exhalation start position null?

		logger.infof(">>>>>>>>>>>sourceControllerEvent at time: %f  agentId: %d", simTimeInSec, scenarioElement.getId());
		return ped;
	}

	private TransmissionModelSourceParameters defineSourceParameters(SourceController controller) {
		int sourceId = controller.getSourceId();
		int defaultSourceId = -1;
		Optional<TransmissionModelSourceParameters> sourceParameters = attributesTransmissionModel
				.getTransmissionModelSourceParameters().stream().filter(s -> s.getSourceId() == sourceId).findFirst();

		// if sourceId not set by user, check if the user has defined default attributes by setting sourceId = -1
		if (sourceParameters.isEmpty()) {
			sourceParameters = attributesTransmissionModel.getTransmissionModelSourceParameters().stream().filter(s -> s.getSourceId() == defaultSourceId).findFirst();

			// if no user defined default values: use attributesTransmissionModel default values
			if (sourceParameters.isPresent()) {
				logger.infof(">>>>>>>>>>>defineSourceParameters: sourceId %d not set explicitly transmissionModelSourceParameters. Source uses default transmissionModelSourceParameters defined for sourceId: %d", sourceId, defaultSourceId);
			} else {
				logger.errorf(">>>>>>>>>>>defineSourceParameters: sourceId %d is not set in transmissionModelSourceParameters", sourceId);
			}
		}
			return sourceParameters.get();
	}

	public AttributesTransmissionModel getAttributesTransmissionModel() {
		return attributesTransmissionModel;
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
