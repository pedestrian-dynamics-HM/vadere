package org.vadere.simulator.models.infection;

import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.control.scenarioelements.SourceController;
import org.vadere.simulator.control.scenarioelements.TopographyController;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.infection.AttributesAirTransmissionModel;
import org.vadere.state.attributes.models.infection.AttributesExposureModel;
import org.vadere.state.attributes.models.infection.AttributesExposureModelSourceParameters;
import org.vadere.state.attributes.scenario.AttributesAerosolCloud;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.attributes.scenario.AttributesDroplets;
import org.vadere.state.health.AirTransmissionModelHealthStatus;
import org.vadere.state.scenario.*;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.logging.Logger;

import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AirTransmissionModel describes the transmission of pathogen from one
 * <code>Pedestrian</code> to another via <code>ParticleDispersion</code> that
 * move through the air.
 * <p>
 *     This particle dispersion can either be described as {@link AerosolCloud
 *     AerosolClouds}, which are carried by air for a longer period, or by {@link
 *     Droplets}, which remain in the air only for short.
 *     Whether aerosol clouds and/or droplets are considered, is defined in
 *     {@link AttributesAirTransmissionModel}.
 * </p>
 * <p> <code>AirTransmissionModel</code> contains the logic, that is:
 * <ul>
 *     <li>Each pedestrian obtains a {@link AirTransmissionModelHealthStatus health
 *     status} after being inserted into the topography.</li>
 *     <li>Infectious pedestrians emit pathogen contained in aerosol
 *     clouds or droplets.</li>
 *     <li>Pedestrians health status, aerosol clouds, droplets</li>
 *     <li>The <code>AirTransmissionModel</code> deletes aerosol clouds and
 *     droplets once they have reached a minimum pathogen concentration.</li>
 * </ul>
 */
@ModelClass
public class AirTransmissionModel extends AbstractExposureModel {

	protected static Logger logger = Logger.getLogger(AirTransmissionModel.class);

	private AttributesAirTransmissionModel attrAirTransmissionModel;
	protected double simTimeStepLength;
	Topography topography;
	int aerosolCloudIdCounter;

	private Map<Integer, Integer> spawnCounter;
	protected Map<AerosolCloud, Integer> aerosolCounter;

	private Map<Integer, VPoint> lastPedestrianPositions;
	private Map<Integer, Vector2D> viewingDirections;
	private Map<Integer, Double> nextDropletsExhalationTime;
	protected static final double MIN_PED_STEP_LENGTH = 0.1;

	/**
	 * Key that is used for initializeVadereContext in ScenarioRun
	 */
	public static final String simStepLength = "simTimeStepLength";

	/**
	 * constant that results from exponential decay of pathogen concentration: C(t) = C_init * exp(-lambda * t),
	 * lambda = exponentialDecayFactor / halfLife
	 */
	static final double exponentialDecayFactor = Math.log(2.0);

	/**
	 * Defines a percentage of the initial pathogen concentration
	 * (pathogenLoad / aerosolCloud.volume); As soon as an aerosolCloud has reached the minimum concentration, the
	 * aerosolCloud is considered negligible and therefore deleted
	 */
	protected static final double minimumPercentage = 0.01;

	/**
	 * Define the simulation steps after which an aerosol cloud gets removed when moving through an obstacle
	 */
	protected static final int STUCK_MAX = 3;
	protected static final int MOVE_AEROSOLS_EVERY_N_STEPS = 3;
	protected int airFlowStepCounter = 0;

	@Override
	public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
		initialize(domain, attributesPedestrian, random);
		setAttributes(attributesList);
	}

	protected void initialize(Domain domain, AttributesAgent attributesPedestrian, Random random) {
		this.domain = domain;
		this.random = random;
		attributesAgent = attributesPedestrian;
		topography = domain.getTopography();
		simTimeStepLength = VadereContext.getCtx(this.topography).getDouble(simStepLength);
		aerosolCloudIdCounter = 1;
		viewingDirections = new HashMap<>();
		lastPedestrianPositions = new HashMap<>();
		nextDropletsExhalationTime = new HashMap<>();

		spawnCounter = new HashMap<>();
		aerosolCounter = new HashMap<>();
	}

	private void setAttributes(List<Attributes> attributesList) {
		attrAirTransmissionModel = Model.findAttributes(attributesList, AttributesAirTransmissionModel.class);
	}

	protected AttributesExposureModel getAttributes() {
		return attrAirTransmissionModel;
	}

	@Override
	public void preLoop(double simTimeInSec) {}

	@Override
	public void postLoop(double simTimeInSec) {}

	@Override
	public void update(double simTimeInSec) {
		AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) getAttributes();

		if (attrModel.isAerosolCloudsActive()) {
			executeAerosolCloudEmissionEvents(simTimeInSec);
			updateAerosolClouds(simTimeInSec);
			updatePedestriansExposureToAerosolClouds();
		}

		if (attrModel.isDropletsActive()) {
			executeDropletEmissionEvents(simTimeInSec);
			updateDroplets(simTimeInSec);
			updatePedestriansExposureToDroplets();
		}

		if (attrModel.isAerosolCloudsActive() || attrModel.isDropletsActive()) {
			updatePedestriansHealthStatus(simTimeInSec);
		}
	}

	@Override
	public void updatePedestrianDegreeOfExposure(Pedestrian pedestrian, double deltaDegreeOfExposure) {
		pedestrian.incrementDegreeOfExposure(deltaDegreeOfExposure);
	}

	public void executeAerosolCloudEmissionEvents(double simTimeInSec) {
		Collection<Pedestrian> infectiousPedestrians = getInfectiousPedestrians(topography);
		for (Pedestrian pedestrian : infectiousPedestrians) {
			createAerosolClouds(simTimeInSec, pedestrian);
		}
	}

	public void executeDropletEmissionEvents(double simTimeInSec) {
		Collection<Pedestrian> infectiousPedestrians = getInfectiousPedestrians(topography);
		for (Pedestrian pedestrian : infectiousPedestrians) {
			createDroplets(simTimeInSec, pedestrian);
		}
	}

	public void updateAerosolClouds(double simTimeInSec) {
		updateAerosolCloudsPathogenLoad(simTimeInSec);
		updateAerosolCloudsExtent();
		updateAerosolCloudsLocation(simTimeInSec);
		deleteExpiredAerosolClouds();
		removeStuckAerosolClouds();
	}

	public void updateDroplets(double simTimeInSec) {
		// dropletsPathogenLoad remains unchanged until deletion
		deleteExpiredDroplets(simTimeInSec);
	}

	public void createAerosolClouds(double simTimeInSec, Pedestrian pedestrian) {
		AirTransmissionModelHealthStatus healthStatus = pedestrian.getHealthStatus();

		if (healthStatus.isStartingExhalation()) {
			healthStatus.setExhalationStartPosition(pedestrian.getPosition());

		} else if (healthStatus.isStartingInhalation()) {
			// VPoint startBreatheOutPosition = healthStatus.getExhalationStartPosition();
			// VPoint stopBreatheOutPosition = pedestrian.getPosition();
			// VLine distanceWalkedDuringExhalation = new VLine(startBreatheOutPosition, stopBreatheOutPosition);
			// VPoint center = distanceWalkedDuringExhalation.midPoint();
			VPoint aerosolCloudCenter = computeAerosolCloudCenter(pedestrian);

			AerosolCloud aerosolCloud = generateAerosolCloud(simTimeInSec, aerosolCloudCenter);
			topography.addAerosolCloud(aerosolCloud);

			healthStatus.resetStartExhalationPosition();
		}
	}

	private VPoint computeAerosolCloudCenter(Pedestrian pedestrian) {
		AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) getAttributes();
		VPoint aerosolCloudCenter;
		// TODO delete next line if necessary
		pedestrian.setSitting(false);
		if (pedestrian.isSitting()) {
			Vector2D aerosolCloudDirection = pedestrian.getSittingDirection().normalize(attrModel.getAerosolCloudInitialRadius());
			aerosolCloudCenter = new VPoint(pedestrian.getPosition().getX() + aerosolCloudDirection.getX(),
					pedestrian.getPosition().getY() + aerosolCloudDirection.getY());
		}
		else {
			VPoint startBreatheOutPosition = pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus().getExhalationStartPosition();
			VPoint stopBreatheOutPosition = pedestrian.getPosition();
			double walkingDirectionX = stopBreatheOutPosition.getX() - startBreatheOutPosition.getX();
			double walkingDirectionY = stopBreatheOutPosition.getY() - startBreatheOutPosition.getY();
			Vector2D aerosolCloudDirection = new Vector2D(walkingDirectionX, walkingDirectionY).normalize(attrModel.getAerosolCloudInitialRadius());

			VLine distanceWalkedDuringExhalation = new VLine(startBreatheOutPosition, stopBreatheOutPosition);
			//aerosolCloudCenter = distanceWalkedDuringExhalation.midPoint();
			VPoint walkingMidPoint = distanceWalkedDuringExhalation.midPoint();
			aerosolCloudCenter = new VPoint(walkingMidPoint.getX() + aerosolCloudDirection.getX(),
					walkingMidPoint.getY() + aerosolCloudDirection.getY());
		}
		return aerosolCloudCenter;
	}

	private AerosolCloud generateAerosolCloud(double simTimeInSec, VPoint aerosolCloudCenter){
		AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) getAttributes();

		AerosolCloud aerosolCloud = new AerosolCloud(new AttributesAerosolCloud(aerosolCloudIdCounter,
				attrModel.getAerosolCloudInitialRadius(),
				aerosolCloudCenter,
				simTimeInSec,
				attrModel.getAerosolCloudInitialPathogenLoad()));

		aerosolCloudIdCounter = aerosolCloudIdCounter + 1;

		return aerosolCloud;
	}

	private void createDroplets(double simTimeInSec, Pedestrian pedestrian) {
		AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) getAttributes();
		int pedestrianId = pedestrian.getId();
		Vector2D viewingDirection;
		VPoint currentPosition = pedestrian.getPosition();
		VPoint lastPosition = lastPedestrianPositions.get(pedestrianId);
		if (lastPedestrianPositions.get(pedestrianId) == null) {
			viewingDirection = new Vector2D(random.nextDouble(), random.nextDouble());
		} else {
			if (lastPosition.distance(currentPosition) < MIN_PED_STEP_LENGTH) {
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
		double dropletExhalationPeriod = 1 / attrModel.getDropletsEmissionFrequency();

		if (nextDropletsExhalationTime.get(pedestrianId) == null) {
			nextDropletsExhalationTime.put(pedestrianId, simTimeInSec + dropletExhalationPeriod);
		} else if (simTimeInSec >= nextDropletsExhalationTime.get(pedestrianId) && !pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus().isBreathingIn()) {
			Droplets droplets = new Droplets(new AttributesDroplets(1,
					simTimeInSec,
					attrModel.getDropletsPathogenLoad(),
					pedestrian.getPosition(),
					viewingDirection,
					attrModel.getDropletsDistanceOfSpread(),
					attrModel.getDropletsAngleOfSpreadInDeg()));

			topography.addDroplets(droplets);

			nextDropletsExhalationTime.put(pedestrianId, simTimeInSec + dropletExhalationPeriod);
		}
	}

	public void updateAerosolCloudsPathogenLoad(double simTimeInSec) {
		AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) getAttributes();
		double lambda = exponentialDecayFactor / attrModel.getAerosolCloudHalfLife();
		double decayFactor = Math.exp(-lambda * simTimeStepLength);

		Collection<AerosolCloud> allAerosolClouds = topography.getAerosolClouds();
		for (AerosolCloud aerosolCloud : allAerosolClouds) {
			if (simTimeInSec > aerosolCloud.getCreationTime()) {
				aerosolCloud.setCurrentPathogenLoad(aerosolCloud.getCurrentPathogenLoad() * decayFactor);
			}
		}
	}

	public void updateAerosolCloudsExtent() {
		AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) getAttributes();
		Collection<AerosolCloud> allAerosolClouds = topography.getAerosolClouds();
		for (AerosolCloud aerosolCloud : allAerosolClouds) {
			double deltaRadius = 0.0;

			/*
			 * Increasing extent due to dispersion, multiplication with simTimeStepLength keeps deltaRadius independent
			 * of simulation step width
			 */
			if (attrModel.getAerosolCloudAirDispersionFactor() > 0) {
				deltaRadius = attrModel.getAerosolCloudAirDispersionFactor() * simTimeStepLength;
			}

			/*
			 * Increasing extent due to moving air caused by agents, multiplication with simTimeStepLength keeps
			 * deltaRadius independent of simulation step width
			 */
			if (attrModel.getAerosolCloudPedestrianDispersionWeight() > 0) {
				Collection<Pedestrian> pedestriansInsideCloud = getPedestriansInsideAerosolCloud(topography, aerosolCloud);
			for (Pedestrian pedestrian : pedestriansInsideCloud) {
				deltaRadius += pedestrian.getVelocity().getLength() * attrModel.getAerosolCloudPedestrianDispersionWeight() * simTimeStepLength;
			}
		}

			aerosolCloud.increaseShape(deltaRadius);
		}
	}

	/**
	 * Deletes aerosol clouds with negligible pathogen concentration, i.e. if current pathogen concentration is smaller
	 * than a threshold (minimumPercentage * initial pathogen concentration)
	 */
	public void deleteExpiredAerosolClouds() {
		AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) getAttributes();
		double initialCloudVolume = AerosolCloud.radiusToVolume(attrModel.getAerosolCloudInitialRadius());
		double initialPathogenConcentration = attrModel.getAerosolCloudInitialPathogenLoad() / initialCloudVolume;
		double minimumConcentration = minimumPercentage * initialPathogenConcentration;

		topography.getAerosolClouds().removeIf(a -> a.getPathogenConcentration() < minimumConcentration);
	}

	public void deleteExpiredDroplets(double simTimeInSec) {
		AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) getAttributes();
		Collection<Droplets> dropletsToBeDeleted = topography.getDroplets()
				.stream()
				.filter(d -> attrModel.getDropletsLifeTime() + d.getCreationTime() < simTimeInSec)
				.collect(Collectors.toSet());
		for (Droplets droplets : dropletsToBeDeleted) {
			topography.getDroplets().remove(droplets);
		}
	}

	protected void updatePedestriansHealthStatus(double simTimeInSec) {
		AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) getAttributes();
		Collection<Pedestrian> allPedestrians = topography.getPedestrianDynamicElements().getElements();
		for (Pedestrian pedestrian : allPedestrians) {
			pedestrian.<AirTransmissionModelHealthStatus>getHealthStatus()
					.updateRespiratoryCycle(simTimeInSec, attrModel.getPedestrianRespiratoryCyclePeriod());
		}
	}

	protected void updatePedestriansExposureToAerosolClouds() {
		AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) getAttributes();

		// Agents absorb pathogen continuously but simulation is discrete. Therefore, the absorption during inhalation
		// must be divided into absorption for each sim step:
		double inhalationPeriodLength = attrModel.getPedestrianRespiratoryCyclePeriod() / 2.0;
		double aerosolAbsorptionRatePerSimStep = attrModel.getAerosolCloudAbsorptionRate() * (simTimeStepLength / inhalationPeriodLength);

		Collection<AerosolCloud> allAerosolClouds = topography.getAerosolClouds();
		for (AerosolCloud aerosolCloud : allAerosolClouds) {
			Collection<Pedestrian> pedsInCloud = getPedestriansInsideAerosolCloud(topography, aerosolCloud);

			for (Pedestrian ped : pedsInCloud) {
				if (ped.<AirTransmissionModelHealthStatus>getHealthStatus().isBreathingIn()) {
					double deltaDegreeOfExposure = aerosolCloud.getPathogenConcentration() * aerosolAbsorptionRatePerSimStep;
					updatePedestrianDegreeOfExposure(ped, deltaDegreeOfExposure);
				}
			}
		}
	}

	protected void updatePedestriansExposureToDroplets() {
		AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) getAttributes();

		/*
		 * Agents absorb pathogen continuously but simulation is discrete. Therefore, the absorption during inhalation
		 * must be divided into absorption for each sim step:
		 */
		double inhalationPeriodLength = attrModel.getPedestrianRespiratoryCyclePeriod() / 2.0;
		double dropletsAbsorptionRatePerSimStep = attrModel.getDropletsAbsorptionRate() * (simTimeStepLength / inhalationPeriodLength);

		/*
		 * Intake of droplets: Inhaling agents simply absorb a fraction of the pathogen from droplets they are exposed
		 * to. In contrast to intake of pathogen from aerosol clouds, we do not consider concentrations (for simplicity
		 * or to avoid further assumptions on pathogen distribution within droplets).
		 */
		Collection<Droplets> allDroplets = topography.getDroplets();
		for (Droplets droplets : allDroplets) {
			Rectangle2D bounds = droplets.getShape().getBounds2D();
			VPoint center = new VPoint(bounds.getCenterX(), bounds.getCenterY());
			double proximity = Math.max(bounds.getHeight(), bounds.getWidth());

			Collection<Pedestrian> nearbyPeds = topography.getSpatialMap(Pedestrian.class).getObjects(center, proximity);

			for (Pedestrian ped : nearbyPeds) {
				if (droplets.getShape().contains(ped.getPosition()) &&
						ped.<AirTransmissionModelHealthStatus>getHealthStatus().isBreathingIn()) {

					double deltaDegreeOfExposure = attrModel.getDropletsPathogenLoad() * dropletsAbsorptionRatePerSimStep;
					updatePedestrianDegreeOfExposure(ped, deltaDegreeOfExposure);
				}
			}
		}
	}

	public Collection<Pedestrian> getInfectiousPedestrians(Topography topography) {
		return topography.getPedestrianDynamicElements()
				.getElements()
				.stream()
				.filter(Pedestrian::isInfectious)
				.collect(Collectors.toSet());
	}

	@Override
	public Agent sourceControllerEvent(SourceController controller, double simTimeInSec, Agent scenarioElement) {
		AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) getAttributes();
		AttributesExposureModelSourceParameters sourceParameters = defineSourceParameters(controller, attrModel);

		Pedestrian ped = (Pedestrian) scenarioElement;
		AirTransmissionModelHealthStatus healthStatus = new AirTransmissionModelHealthStatus();
		healthStatus.setRespiratoryTimeOffset(random.nextDouble() * attrModel.getPedestrianRespiratoryCyclePeriod());
		healthStatus.setBreathingIn(false);
		ped.setHealthStatus(healthStatus);

		if (!spawnCounter.containsKey(controller.getSourceId())) {
			spawnCounter.put(controller.getSourceId(), 0);
		}
		int counter = spawnCounter.get(controller.getSourceId());
		if (!sourceParameters.getInfectiousSpawnIds().isEmpty() && sourceParameters.getInfectiousSpawnIds().get(0) == counter) {
			ped.setInfectious(sourceParameters.isInfectious());
			sourceParameters.getInfectiousSpawnIds().remove(0);
		}
		spawnCounter.put(controller.getSourceId(), counter + 1);

		return ped;
	}

	@Override
	public Pedestrian topographyControllerEvent(TopographyController topographyController, double simTimeInSec, Agent agent) {
		AttributesAirTransmissionModel attrModel = (AttributesAirTransmissionModel) getAttributes();
		Pedestrian pedestrian = (Pedestrian) agent;
		AirTransmissionModelHealthStatus defaultHealthStatus = new AirTransmissionModelHealthStatus();
		defaultHealthStatus.setRespiratoryTimeOffset(random.nextDouble() * attrModel.getPedestrianRespiratoryCyclePeriod());
		pedestrian.setHealthStatus(defaultHealthStatus);

		if (attrModel.getInfectiousPedestrianIdsNoSource().contains(agent.getId())) {
			pedestrian.setInfectious(true);
		}

		return pedestrian;
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

	public void updateAerosolCloudsLocation(double simTimeInSec) {
		if (topography.getAirFlow() == null) {
			return;
		}
		airFlowStepCounter++;
		if (airFlowStepCounter >= MOVE_AEROSOLS_EVERY_N_STEPS) {
			Collection<AerosolCloud> allAerosolClouds = topography.getAerosolClouds();
			Collection<AerosolCloud> toRemove = new ArrayList<>();
			for (AerosolCloud aerosolCloud : allAerosolClouds) {
				VPoint center = aerosolCloud.getCenter();
				double[] airflowXY = topography.getAirFlow().getFlowDirection(simTimeInSec, center.getX(), center.getY());
				double xShift = airflowXY[0] * simTimeStepLength * MOVE_AEROSOLS_EVERY_N_STEPS;
				double yShift = airflowXY[1] * simTimeStepLength * MOVE_AEROSOLS_EVERY_N_STEPS;
				// remove aerosol clouds that are shifted outside the airflow bounds
				if (topography.getAirFlow().shouldRemoveAerosolCloud(center.getX(), center.getY(), xShift, yShift)) {
					toRemove.add(aerosolCloud);
				}
				aerosolCloud.shiftShape(xShift, yShift);
			}
			allAerosolClouds.removeAll(toRemove);
			airFlowStepCounter = 0;
		}
	}

	public void removeStuckAerosolClouds() {
		if (topography.getAirFlow() == null) {
			return;
		}
		// only remove stuck AerosolClouds after they should have been moved
		if (airFlowStepCounter != 0) {
			return;
		}
		Collection<AerosolCloud> aerosolClouds = topography.getAerosolClouds();
		Collection<Integer> blockingIDs = topography.getAirFlow().getBlockingObstaclesIDs();
		List<Obstacle> blockingObstacles = topography.getObstacles().stream()
				.filter(obstacle -> blockingIDs.contains(obstacle.getId()))
				.collect(Collectors.toList());
		Collection<AerosolCloud> toRemove = new ArrayList<>();
		for (AerosolCloud aerosolCloud : aerosolClouds) {
			boolean isStuck = blockingObstacles.stream()
					.anyMatch(obstacle -> obstacle.getShape().contains(aerosolCloud.getCenter()));
			if (isStuck) {
				if (aerosolCounter.containsKey(aerosolCloud)) {
					aerosolCounter.put(aerosolCloud, aerosolCounter.get(aerosolCloud) + 1);

					if (aerosolCounter.get(aerosolCloud) > STUCK_MAX) {
						aerosolCounter.remove(aerosolCloud);
						toRemove.add(aerosolCloud);
					}
				} else {
					aerosolCounter.put(aerosolCloud, 1);
				}
			}  else {
				aerosolCounter.remove(aerosolCloud);
			}
		}
		aerosolClouds.removeAll(toRemove);
	}
}
